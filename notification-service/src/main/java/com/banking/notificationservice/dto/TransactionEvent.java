package com.banking.notificationservice.dto;

import java.math.BigDecimal;

public record TransactionEvent(
        String eventId,
        String eventType,
        Long customerId,
        Long receiverId,
        String transactionId,
        String status,
        BigDecimal amount,
        String currency
) {

}
