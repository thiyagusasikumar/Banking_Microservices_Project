package com.banking.notificationservice.dto;

import java.time.Instant;
import java.time.LocalDateTime;

public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path
) {
}
