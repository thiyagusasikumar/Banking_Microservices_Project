package com.banking.accountservice.repository;

import com.banking.accountservice.entity.AccountOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface OutboxRepository extends JpaRepository<AccountOutbox,Long> {
    @Query(value = """
            SELECT *
            FROM account_outbox
            WHERE status = 'PENDING'
              AND next_retry_at <= :now
            ORDER BY created_at, id
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<AccountOutbox> lockNextPendingBatch(
            @Param("now") Instant now
    );
}
