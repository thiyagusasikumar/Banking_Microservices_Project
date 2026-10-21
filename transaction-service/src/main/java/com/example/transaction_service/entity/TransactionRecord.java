package com.example.transaction_service.entity;

import com.example.transaction_service.constants.Status;
import com.example.transaction_service.constants.Type;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;


@Data
@Entity
@AllArgsConstructor
@RequiredArgsConstructor
@Table(name = "transactions", uniqueConstraints = {
        @UniqueConstraint(name = "uk_transactions_idempotency_key", columnNames = "idempotency_key"),
        @UniqueConstraint(name = "uk_transactions_reference", columnNames = "transaction_reference")
})
public class TransactionRecord {
    @Id
    private UUID id;

    @Column(name = "transaction_reference")
    private String reference;

    @Column(name = "source_account")
    private String sourceAccount;

    @Column(name = "destination_account")
    private String destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private Type type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "failure_reason")
    private String failureReason;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Version
    private long version;

}
