package com.banking.authservice.repository;

import com.banking.authservice.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent,Long> {
    @Query(value = """
            SELECT *
            FROM outbox_events
            WHERE status = 'PENDING'
              AND next_retry_at <= :now
            ORDER BY created_at, id
            LIMIT 100
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<OutboxEvent> lockNextPendingBatch(@Param("now") LocalDateTime now);
}
