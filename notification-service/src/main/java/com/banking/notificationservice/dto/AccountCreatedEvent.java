package com.banking.notificationservice.dto;

import java.time.LocalDateTime;

public record AccountCreatedEvent(
        String eventId,
        String eventType,
        Long customerId,
        Long accountId,
        String accountNumber,
        String accountType,
        LocalDateTime occurredAt
) {
}



