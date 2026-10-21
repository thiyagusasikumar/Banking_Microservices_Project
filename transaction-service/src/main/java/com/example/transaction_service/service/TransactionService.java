package com.example.transaction_service.service;

import com.example.transaction_service.constants.Status;
import com.example.transaction_service.constants.Type;
import com.example.transaction_service.entity.TransactionRecord;
import com.example.transaction_service.repository.TransactionRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Currency;
import java.util.InputMismatchException;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class TransactionService {

    private final TransactionRepository transactions;
    private final TransactionTemplate transactionTemplate;

    public TransactionService(TransactionRepository transactions, TransactionTemplate transactionTemplate) {
        this.transactions = transactions;
        this.transactionTemplate = transactionTemplate;
    }


public TransactionRecord create(Type type, String source, String destination, BigDecimal amount, String currency, String key) {

        validate(type, source, destination, amount, currency, key);

        String finalSource = source == null ? null : source.trim();

        String finalDestination = destination == null ? null : destination.trim();

        String finalCurrency = currency.trim().toUpperCase();

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

        TransactionRecord record;

        try {
            record = transactionTemplate.execute(status -> transactions.saveAndFlush(pending));
        } catch (DataIntegrityViolationException ex) {
            TransactionRecord duplicate = transactions.findByIdempotencyKey(key)
                    .orElseThrow(() -> new DuplicateKeyException("Idempotency key is being processed"));
            return sameRequest(duplicate, type, finalSource, finalDestination, finalAmount, finalCurrency);
        }

        if (record == null) {
            throw new DuplicateKeyException("Idempotency key is being processed");
        }

        return record;
    }


    private TransactionRecord sameRequest(TransactionRecord existing, Type type, String source,
                                          String destination, BigDecimal amount, String currency) {

        boolean matches = existing.getType() == type
                && existing.getAmount().compareTo(amount) == 0
                && Objects.equals(existing.getSourceAccount(), source)
                && Objects.equals(existing.getDestinationAccount(), destination)
                && Objects.equals(existing.getCurrency(), currency);

        if (!matches) {
            throw new DuplicateKeyException("Idempotency-Key was already used with a different request");
        }

        return existing;
    }


    private void validate(Type type, String source, String destination,
                          BigDecimal amount, String currency, String key) {

        if (key == null || key.isBlank() || key.length() > 128) {

            throw new InputMismatchException("Idempotency-Key must have 1 to 128 characters");

        }

        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {

            throw new InputMismatchException("Amount must be positive with at most 2 decimal places");

        }

        if (currency == null || currency.isBlank()) {

            throw new InputMismatchException("Currency is required");

        }

        try {
            Currency.getInstance(currency.toUpperCase(Locale.ROOT));
        }
        catch (RuntimeException ex) {
            throw new InputMismatchException("Invalid currency");
        }

        if (type == null) {
            throw new InputMismatchException("Transaction type is required");
        }

        switch (type) {
            case DEPOSIT -> {
                if (destination == null || destination.isBlank()) {
                    throw new InputMismatchException("Account is required");
                }
            }
            case WITHDRAWAL -> {
                if (source == null || source.isBlank()) {
                    throw new InputMismatchException("Account is required");
                }
            }
            case TRANSFER -> {
                if (source == null || source.isBlank() || destination == null || destination.isBlank()) {
                    throw new InputMismatchException("Source and destination accounts are required");
                }
            }
        }

    }

}
