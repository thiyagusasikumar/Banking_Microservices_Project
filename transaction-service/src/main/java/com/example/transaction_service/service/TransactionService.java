package com.example.transaction_service.service;

   import com.example.transaction_service.client.AccountServiceClient;
import com.example.transaction_service.constants.Status;
import com.example.transaction_service.constants.Type;
import com.example.transaction_service.dto.AccountOperationRequest;
import com.example.transaction_service.dto.AccountOperationResponse;
import com.example.transaction_service.entity.TransactionRecord;
import com.example.transaction_service.exception.IdempotencyConflictException;
import com.example.transaction_service.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final TransactionTemplate transactionTemplate;
    private final AccountServiceClient accountServiceClient;

    public TransactionService(TransactionRepository transactions, TransactionTemplate transactionTemplate, AccountServiceClient accountServiceClient) {
        this.transactions = transactions;
        this.transactionTemplate = transactionTemplate;
        this.accountServiceClient = accountServiceClient;
    }

    public TransactionRecord create(Type type, String source, String destination, BigDecimal amount, String currency, String key) {

        validate(type, source, destination, amount, currency, key);

        String finalSource = source == null ? null : source.trim();

        String finalDestination = destination == null ? null : destination.trim();

        String finalCurrency = currency.trim().toUpperCase(Locale.ROOT);

        BigDecimal finalAmount = amount.setScale(2);

        TransactionRecord existing = transactions.findByIdempotencyKey(key).orElse(null);

        if (existing != null) {
            return sameRequest(existing, type, finalSource, finalDestination, finalAmount, finalCurrency);
        }

        TransactionRecord pending = new TransactionRecord();
        pending.setId(UUID.randomUUID());
        pending.setReference(UUID.randomUUID().toString());
        pending.setType(type);
        pending.setSourceAccount(finalSource);
        pending.setDestinationAccount(finalDestination);
        pending.setAmount(finalAmount);
        pending.setCurrency(finalCurrency);
        pending.setStatus(Status.INITIATED);
        pending.setIdempotencyKey(key);
        Instant now = Instant.now();
        pending.setCreatedAt(now);
        pending.setUpdatedAt(now);

        try {
            transactionTemplate.execute(status -> transactions.saveAndFlush(pending));
        } catch (DataIntegrityViolationException ex) {
            TransactionRecord duplicate = transactions.findByIdempotencyKey(key).orElseThrow(() -> ex);
            return sameRequest(duplicate, type, finalSource, finalDestination, finalAmount, finalCurrency);
        }

        // Each local commit is short. No transaction is held while waiting for HTTP.
        updateStatus(pending.getReference(), Status.PROCESSING, null);
        AccountOperationRequest operation = new AccountOperationRequest(pending.getReference(), type,
                parseAccount(finalSource), parseAccount(finalDestination), finalAmount, finalCurrency);
        try {
            return applyResponse(pending.getReference(), accountServiceClient.post(operation));
        } catch (RestClientException ex) {
            // The POST may have committed even when its response was lost. Query only; never re-post here.
            return reconcile(pending.getReference());
        }
    }

    public TransactionRecord get(String reference) {
        return transactions.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    public TransactionRecord reconcile(String reference) {
        TransactionRecord record = get(reference);
        if (terminal(record.getStatus())) {
            return record;
        }
        try {
            return applyResponse(reference, accountServiceClient.get(reference));
        } catch (HttpClientErrorException.NotFound ex) {
            // A prior POST could still be in flight. Absence is not proof that it is safe to post again.
            return updateStatus(reference, Status.PENDING_RECONCILIATION, "Posting not yet confirmed");
        } catch (RestClientException ex) {
            return updateStatus(reference, Status.PENDING_RECONCILIATION, "Account Service unavailable for reconciliation");
        }
    }

    private TransactionRecord applyResponse(String reference, AccountOperationResponse result) {
        if (result == null || !reference.equals(result.transactionReference()) || result.status() == null) {
            return updateStatus(reference, Status.PENDING_RECONCILIATION, "Invalid posting response");
        }
        return switch (result.status()) {
            case APPLIED -> updateStatus(reference, Status.SUCCESS, null);
            case REJECTED -> updateStatus(reference, Status.FAILED,
                    result.failureReason() == null ? "Account Service rejected the operation" : result.failureReason());
        };
    }

    private TransactionRecord updateStatus(String reference, Status newStatus, String reason) {
        return transactionTemplate.execute(status -> {
            TransactionRecord record = transactions.findByReferenceForUpdate(reference)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
            if (!terminal(record.getStatus())) {
                record.setStatus(newStatus);
                record.setFailureReason(reason);
                record.setUpdatedAt(Instant.now());
            }
            return record;
        });
    }

    private boolean terminal(Status status) {
        return status == Status.SUCCESS || status == Status.COMPLETED || status == Status.FAILED;
    }

    private TransactionRecord sameRequest(TransactionRecord existing, Type type, String source,
                                          String destination, BigDecimal amount, String currency) {
        boolean matches = existing.getType() == type
                && existing.getAmount().compareTo(amount) == 0
                && Objects.equals(existing.getSourceAccount(), source)
                && Objects.equals(existing.getDestinationAccount(), destination)
                && Objects.equals(existing.getCurrency(), currency);
        if (!matches) {
            throw new IdempotencyConflictException("Idempotency-Key was already used with a different request");
        }
        return existing;
    }

    private void validate(Type type, String source, String destination,
                          BigDecimal amount, String currency, String key) {
        if (key == null || key.isBlank() || key.length() > 128) {
            throw badRequest("Idempotency-Key must have 1 to 128 characters");
        }
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17) {
            throw badRequest("Amount must be positive with at most 17 integer and 2 decimal digits");
        }
        if (currency == null || currency.isBlank()) {
            throw badRequest("Currency is required");
        }
        try {
            Currency.getInstance(currency.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException ex) {
            throw badRequest("Invalid currency");
        }
        if (type == null) {
            throw badRequest("Transaction type is required");
        }
        switch (type) {
            case DEPOSIT -> requireAccount(destination);
            case WITHDRAWAL -> requireAccount(source);
            case TRANSFER -> {
                Long sourceId = requireAccount(source);
                Long destinationId = requireAccount(destination);
                if (sourceId.equals(destinationId)) {
                    throw badRequest("Source and destination accounts must differ");
                }
            }
        }
    }

    private Long requireAccount(String accountId) {
        Long id = parseAccount(accountId);
        if (id == null || id <= 0) {
            throw badRequest("Account ID must be a positive number");
        }
        return id;
    }

    private Long parseAccount(String accountId) {
        if (accountId == null) {
            return null;
        }
        try {
            return Long.valueOf(accountId.trim());
        } catch (NumberFormatException ex) {
            throw badRequest("Account ID must be a positive number");
        }
    }

    private ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
