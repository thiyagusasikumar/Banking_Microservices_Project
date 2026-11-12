package com.banking.notificationservice.dto;

import java.time.Instant;

public record AccountEvent(
        String eventId,
        String eventType,
        Long customerId,
        Long accountId,
        String accountNumber,
        String accountType,
        Instant occurredAt
) {
}



