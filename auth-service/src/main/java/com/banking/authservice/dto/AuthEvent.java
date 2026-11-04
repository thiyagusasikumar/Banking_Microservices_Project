package com.banking.authservice.dto;

import java.time.Instant;

public record AuthEvent (
        String eventId,
        String eventType,
        Long customerId,
        Instant occurredAt
){
}
