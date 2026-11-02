package com.banking.notificationservice.dto;

import java.time.LocalDateTime;

public record AuthEvent(
        String eventId,
        String eventType,
        Long customerId,
        LocalDateTime occurredAt
) {
}
