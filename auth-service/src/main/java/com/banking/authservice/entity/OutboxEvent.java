package com.banking.authservice.entity;

import com.banking.authservice.constants.OutboxStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;


@Entity
@RequiredArgsConstructor
@Getter
@Setter
@Table(
        name = "outbox_events",
        indexes = {
                @Index(
                        name = "idx_outbox_status_created",
                        columnList = "status, created_at"
                )
        }
)
public class OutboxEvent {

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Id
    private Long id;

    @Column(unique = true, nullable = false)
    private String eventId;

    @Column(nullable = false)
    private String aggregationType;

    @Column(nullable = false)
    private String aggregateId;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false,columnDefinition = "Text")
    private String payLoad;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;

    @Column(nullable = false)
    private int retryCount;

    private LocalDateTime createdAt;

    private LocalDateTime publishedAt;

    private LocalDateTime nextRetryAt;

    @Column(columnDefinition = "Text")
    private String lastErrorMessage;


    @PrePersist
    public void onCreate() {
        createdAt = LocalDateTime.now();

        if (status == null) {
            status = OutboxStatus.PENDING;
        }

        if (retryCount < 0) {
            retryCount = 0;
        }
    }

}
