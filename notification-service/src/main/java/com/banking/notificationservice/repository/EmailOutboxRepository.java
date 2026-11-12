package com.banking.notificationservice.repository;


import com.banking.notificationservice.constants.EmailStatus;
import com.banking.notificationservice.constants.EmailType;
import com.banking.notificationservice.entity.EmailOutbox;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface EmailOutboxRepository extends JpaRepository<EmailOutbox,Long> {

    List<EmailOutbox> findTop100ByEmailStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
            EmailStatus emailStatus,
            Instant nextRetryAt
    );


    boolean existsByEventIdAndEmailType(String eventId, EmailType emailType);
}
