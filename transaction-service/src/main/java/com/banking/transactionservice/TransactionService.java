package com.banking.transactionservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Currency;
import java.util.HexFormat;
import java.util.Map;

@Service
public class TransactionService {
    private final TransactionRepository transactions;
    private final OutboxRepository outbox;
    private final AccountClient accountClient;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper mapper;

    public TransactionService(TransactionRepository transactions, OutboxRepository outbox,
                              AccountClient accountClient, TransactionTemplate transactionTemplate, ObjectMapper mapper) {
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
        if (existing != null) return sameRequest(existing, hash);fff

        TransactionRecord record;
        try {
            String finalSource = source;
            String finalDestination = destination;
            BigDecimal finalAmount = amount;
            String finalCurrency = currency;
            record = transactionTemplate.execute(status -> {
                TransactionRecord created = transactions.saveAndFlush(new TransactionRecord(type, finalSource,
                        finalDestination, finalAmount, finalCurrency, key, hash));
                outbox.save(new OutboxEvent(created.getReference(), "TransactionCreated", payload(created)));
                return created;
            });
        } catch (DataIntegrityViolationException ex) {
            TransactionRecord duplicate = transactions.findByIdempotencyKey(key)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key is being processed"));
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
            outbox.save(new OutboxEvent(current.getReference(), eventType, payload(current)));
            return current;
        });
    }

    public TransactionRecord get(String reference) {
        return transactions.findByReference(reference)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
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
                    ? "TransactionCompleted" : "TransactionFailed", payload(latest)));
            return latest;
        });
    }

    private TransactionRecord sameRequest(TransactionRecord existing, String hash) {
        if (!existing.getRequestHash().equals(hash))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Idempotency key belongs to a different request");
        return existing;
    }

    private void validate(TransactionRecord.Type type, String source, String destination,
                          BigDecimal amount, String currency, String key) {
        if (key == null || key.isBlank() || key.length() > 128)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Idempotency-Key must have 1 to 128 characters");
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2
                || amount.precision() - amount.scale() > 17)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Amount must be positive with at most two decimal places");
        try { Currency.getInstance(currency.toUpperCase(java.util.Locale.ROOT)); }
        catch (RuntimeException ex) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid currency"); }
        if (type == TransactionRecord.Type.DEPOSIT && !validAccount(destination)
                || type == TransactionRecord.Type.WITHDRAWAL && !validAccount(source)
                || type == TransactionRecord.Type.TRANSFER && (!validAccount(source) || !validAccount(destination)
                || source.trim().equals(destination.trim())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid account selection");
    }

    private boolean validAccount(String value) { return value != null && !value.isBlank() && value.trim().length() <= 64; }
    private String payload(TransactionRecord record) {
        try {
            return mapper.writeValueAsString(Map.of(
                    "eventId", java.util.UUID.randomUUID().toString(),
                    "transactionReference", record.getReference(),
                    "type", record.getType().name(),
                    "status", record.getStatus().name(),
                    "amount", record.getAmount(),
                    "currency", record.getCurrency(),
                    "sourceAccount", record.getSourceAccount() == null ? "" : record.getSourceAccount(),
                    "destinationAccount", record.getDestinationAccount() == null ? "" : record.getDestinationAccount()));
        } catch (JsonProcessingException ex) { throw new IllegalStateException("Could not serialize event", ex); }
    }
    private String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException ex) { throw new IllegalStateException(ex); }
    }
}
