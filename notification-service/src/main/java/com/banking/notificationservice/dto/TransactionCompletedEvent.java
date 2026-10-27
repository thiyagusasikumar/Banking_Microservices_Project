package com.banking.notificationservice.dto;

import java.math.BigDecimal;

public record TransactionCompletedEvent (
        String eventId,
        Long customerId,
        String transactionId,
        String status,
        BigDecimal amount,
        String currency
){
}
