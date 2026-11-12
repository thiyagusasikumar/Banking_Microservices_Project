package com.banking.notificationservice.entity;

import com.banking.notificationservice.constants.EmailStatus;
import com.banking.notificationservice.constants.EmailType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "email_outbox",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_email_outbox_event_type",
                        columnNames = {"event_id", "email_type"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_email_outbox_status_retry",
                        columnList = "email_status, next_retry_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmailOutbox {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String recipientEmail;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailType emailType;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EmailStatus emailStatus;

    @Column(nullable = false)
    private int retryCount;

    private Instant nextRetryAt;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @Column(nullable = false)
    private Instant createdAt;

    private Instant sentAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();

        if (emailStatus == null) {
            emailStatus = EmailStatus.PENDING;
        }

        if (retryCount < 0) {
            retryCount = 0;
        }
    }

}
