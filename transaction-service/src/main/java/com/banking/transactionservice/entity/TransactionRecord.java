package com.banking.transactionservice.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_idempotency_key", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_transactions_reference", columnNames = "transaction_reference")
})
public class TransactionRecord {
    @Id
    private UUID id;
    @Column(name = "transaction_reference", nullable = false, length = 36)
    private String reference;
    @Column(name = "source_account", length = 64)
    private String sourceAccount;
    @Column(name = "destination_account", length = 64)
    private String destinationAccount;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency;
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 20)
    private Type type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private Status status;
    @Column(name = "idempotency_key", nullable = false, length = 128)
    private String idempotencyKey;
    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;
    @Column(name = "failure_reason", length = 500)
    private String failureReason;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @Version
    private long version;

    public enum Type { DEPOSIT, WITHDRAWAL, TRANSFER }
    public enum Status { INITIATED, COMPLETED, FAILED, PENDING_RECONCILIATION }

    protected TransactionRecord() {}

    public TransactionRecord(Type type, String sourceAccount, String destinationAccount,
                             BigDecimal amount, String currency, String idempotencyKey, String requestHash) {
        this.id = UUID.randomUUID();
        this.reference = UUID.randomUUID().toString();
        this.type = type;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.amount = amount;
        this.currency = currency;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.status = Status.INITIATED;
        this.createdAt = Instant.now();
        this.updatedAt = createdAt;
    }

    public void finish(Status status, String reason) {
        this.status = status;
        this.failureReason = reason;
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public String getReference() { return reference; }
    public String getSourceAccount() { return sourceAccount; }
    public String getDestinationAccount() { return destinationAccount; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public Type getType() { return type; }
    public Status getStatus() { return status; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
