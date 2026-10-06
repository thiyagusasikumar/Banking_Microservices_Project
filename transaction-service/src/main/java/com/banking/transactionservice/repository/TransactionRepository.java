package com.banking.transactionservice.repository;

import com.banking.transactionservice.entity.TransactionRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionRecord, UUID> {
    Optional<TransactionRecord> findByIdempotencyKey(String key);
    Optional<TransactionRecord> findByReference(String reference);
}
