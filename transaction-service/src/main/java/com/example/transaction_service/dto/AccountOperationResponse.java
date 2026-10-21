package com.example.transaction_service.dto;

public record AccountOperationResponse(String transactionReference, PostingStatus status, String failureReason) {

}
