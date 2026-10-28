package com.example.transaction_service.repository;


import com.example.transaction_service.entity.TransactionRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionRecord, UUID> {
    Optional<TransactionRecord> findByIdempotencyKey(String key);
    Optional<TransactionRecord> findByReference(String reference);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TransactionRecord t where t.reference = :reference")
    Optional<TransactionRecord> findByReferenceForUpdate(@Param("reference") String reference);
}
