package com.banking.notificationservice.repository;


import com.banking.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}
