package com.banking.notificationservice.dto;

public record TransactionCompletedEvent (
        String eventId,
        Long customerId,
        String transactionId,
        String status,
        String amount,
        String currency
){
}
