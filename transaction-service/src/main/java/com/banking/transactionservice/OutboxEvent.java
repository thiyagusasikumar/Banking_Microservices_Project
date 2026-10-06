package com.banking.transactionservice;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transaction_outbox")
public class OutboxEvent {
    @Id
    private UUID id;
    @Column(name = "transaction_reference", nullable = false, length = 36)
    private String transactionReference;
    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;
    @Column(nullable = false, columnDefinition = "text")
    private String payload;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    @Column(name = "published_at")
    private Instant publishedAt;

    protected OutboxEvent() {}
    public OutboxEvent(String reference, String eventType, String payload) {
        this.id = UUID.randomUUID();
        this.transactionReference = reference;
        this.eventType = eventType;
        this.payload = payload;
        this.createdAt = Instant.now();
    }
    public UUID getId() { return id; }
    public String getTransactionReference() { return transactionReference; }
    public String getEventType() { return eventType; }
    public String getPayload() { return payload; }
    public Instant getPublishedAt() { return publishedAt; }
    public void markPublished() { publishedAt = Instant.now(); }
}
