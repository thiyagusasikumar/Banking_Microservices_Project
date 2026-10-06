package com.banking.transactionservice.service;

import com.banking.transactionservice.entity.TransactionRecord;
import com.banking.transactionservice.entity.OutboxEvent;
import com.banking.transactionservice.repository.TransactionRepository;
import com.banking.transactionservice.repository.OutboxRepository;
import com.banking.transactionservice.mapper.TransactionMapper;
import com.banking.transactionservice.exception.IdempotencyConflictException;
import com.banking.transactionservice.exception.InvalidTransactionException;
import com.banking.transactionservice.exception.TransactionNotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Currency;
import java.util.HexFormat;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final OutboxRepository outbox;
    private final AccountClient accountClient;
    private final TransactionTemplate transactionTemplate;
    private final TransactionMapper mapper;

    public TransactionService(TransactionRepository transactions, OutboxRepository outbox,
                              AccountClient accountClient, TransactionTemplate transactionTemplate, TransactionMapper mapper) {
        this.transactions = transactions;
        this.outbox = outbox;
        this.accountClient = accountClient;
        this.transactionTemplate = transactionTemplate;
        this.mapper = mapper;
    }

    public TransactionRecord create(TransactionRecord.Type type, String source, String destination,
                                    BigDecimal amount, String currency, String key) {
        validate(type, source, destination, amount, currency, key);
        source = source == null ? null : source.trim();
        destination = destination == null ? null : destination.trim();
        currency = currency.toUpperCase(java.util.Locale.ROOT);
        amount = amount.setScale(2);
        String hash = hash(type + "|" + source + "|" + destination + "|" + amount + "|" + currency);
        TransactionRecord existing = transactions.findByIdempotencyKey(key).orElse(null);
        if (existing != null) return sameRequest(existing, hash);

        TransactionRecord record;
        try {
            String finalSource = source;
            String finalDestination = destination;
            BigDecimal finalAmount = amount;
            String finalCurrency = currency;
            record = transactionTemplate.execute(status -> {
                TransactionRecord created = transactions.saveAndFlush(new TransactionRecord(type, finalSource,
                        finalDestination, finalAmount, finalCurrency, key, hash));
                outbox.save(new OutboxEvent(created.getReference(), "TransactionCreated", mapper.toEventPayload(created)));
                return created;
            });
        } catch (DataIntegrityViolationException ex) {
            TransactionRecord duplicate = transactions.findByIdempotencyKey(key)
                    .orElseThrow(() -> new IdempotencyConflictException("Idempotency key is being processed"));
            return sameRequest(duplicate, hash);
        }
        if (record == null) throw new IllegalStateException("Could not create transaction");

        AccountClient.Outcome outcome;
        try {
            outcome = accountClient.apply(record.getReference(), type, source, destination, amount, currency);
        } catch (RuntimeException ex) {
            outcome = AccountClient.Outcome.UNKNOWN;
        }
        TransactionRecord.Status finalStatus = switch (outcome) {
            case APPLIED -> TransactionRecord.Status.COMPLETED;
            case REJECTED -> TransactionRecord.Status.FAILED;
            case UNKNOWN -> TransactionRecord.Status.PENDING_RECONCILIATION;
        };
        String reason = switch (outcome) {
            case APPLIED -> null;
            case REJECTED -> "Account operation rejected";
            case UNKNOWN -> "Account operation result is unknown; reconcile by transaction reference";
        };
        return transactionTemplate.execute(status -> {
            TransactionRecord current = transactions.findById(record.getId()).orElseThrow();
            if (current.getStatus() != TransactionRecord.Status.INITIATED) return current;
            current.finish(finalStatus, reason);
            transactions.save(current);
            String eventType = switch (finalStatus) {
                case COMPLETED -> "TransactionCompleted";
                case FAILED -> "TransactionFailed";
                case PENDING_RECONCILIATION -> "TransactionPendingReconciliation";
                default -> throw new IllegalStateException();
            };
            outbox.save(new OutboxEvent(current.getReference(), eventType, mapper.toEventPayload(current)));
            return current;
        });
    }

    public TransactionRecord get(String reference) {
        return transactions.findByReference(reference)
                .orElseThrow(() -> new TransactionNotFoundException(reference));
    }

    public TransactionRecord reconcile(String reference) {
        TransactionRecord current = get(reference);
        if (current.getStatus() != TransactionRecord.Status.PENDING_RECONCILIATION
                && current.getStatus() != TransactionRecord.Status.INITIATED) return current;
        AccountClient.Outcome observed;
        try { observed = accountClient.lookup(reference); }
        catch (RuntimeException ex) { observed = AccountClient.Outcome.UNKNOWN; }
        final AccountClient.Outcome outcome = observed;
        if (outcome == AccountClient.Outcome.UNKNOWN) return current;
        return transactionTemplate.execute(status -> {
            TransactionRecord latest = transactions.findById(current.getId()).orElseThrow();
            if (latest.getStatus() != TransactionRecord.Status.PENDING_RECONCILIATION
                    && latest.getStatus() != TransactionRecord.Status.INITIATED) return latest;
            latest.finish(outcome == AccountClient.Outcome.APPLIED
                    ? TransactionRecord.Status.COMPLETED : TransactionRecord.Status.FAILED,
                    outcome == AccountClient.Outcome.APPLIED ? null : "Account operation rejected");
            transactions.save(latest);
            outbox.save(new OutboxEvent(latest.getReference(), outcome == AccountClient.Outcome.APPLIED
                    ? "TransactionCompleted" : "TransactionFailed", mapper.toEventPayload(latest)));
            return latest;
        });
    }

    private TransactionRecord sameRequest(TransactionRecord existing, String hash) {
        if (!existing.getRequestHash().equals(hash))
            throw new IdempotencyConflictException("Idempotency key belongs to a different request");
        return existing;
    }

    private void validate(TransactionRecord.Type type, String source, String destination,
                          BigDecimal amount, String currency, String key) {
        if (key == null || key.isBlank() || key.length() > 128)
            throw new InvalidTransactionException("Idempotency-Key must have 1 to 128 characters");
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17)
            throw new InvalidTransactionException("Amount must be positive with at most two decimal places");
        try { Currency.getInstance(currency.toUpperCase(java.util.Locale.ROOT)); }
        catch (RuntimeException ex) { throw new InvalidTransactionException("Invalid currency"); }
        if (type == TransactionRecord.Type.DEPOSIT && !validAccount(destination)
                || type == TransactionRecord.Type.WITHDRAWAL && !validAccount(source)
                || type == TransactionRecord.Type.TRANSFER && (!validAccount(source) || !validAccount(destination)
                || source.trim().equals(destination.trim())))
            throw new InvalidTransactionException("Invalid account selection");
    }

    private boolean validAccount(String value) { return value != null && !value.isBlank() && value.trim().length() <= 64; }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
