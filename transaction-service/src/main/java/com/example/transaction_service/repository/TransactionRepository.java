package com.example.transaction_service.repository;


import com.example.transaction_service.entity.TransactionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionRecord, UUID> {
    Optional<TransactionRecord> findByIdempotencyKey(String key);
    Optional<TransactionRecord> findByReference(String reference);
}
