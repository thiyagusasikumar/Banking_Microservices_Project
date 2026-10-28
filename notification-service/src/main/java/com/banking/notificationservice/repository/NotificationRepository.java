package com.banking.notificationservice.repository;


import com.banking.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    @Query("""
    SELECT n FROM Notification n
    WHERE n.recipientId = :recipientId
      AND (:query IS NULL OR :query = ''
           OR LOWER(n.title) LIKE LOWER(CONCAT('%', :query, '%'))
           OR LOWER(n.message) LIKE LOWER(CONCAT('%', :query, '%')))
    """)
    Page<Notification> searchNotification(
            @Param("recipientId") Long recipientId,
            @Param("query") String query,
            Pageable pageable
    );

    Optional<Notification> findByIdAndRecipientId(Long id,Long recipientId);

    @Modifying
    @Query("""
        UPDATE Notification n
        SET n.read = true,
            n.readAt = :readAt
        WHERE n.recipientId = :customerId
          AND n.read = false
    """)
    int markAllAsRead(
            @Param("customerId") Long customerId,
            @Param("readAt") LocalDateTime readAt
    );


    boolean existsByEventIdAndRecipientId(String eventId, Long recipientId);


    long countByRecipientIdAndReadFalse(Long recipientId);
}
