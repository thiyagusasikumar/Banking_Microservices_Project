package com.example.transaction_service.dto;

import com.example.transaction_service.constants.PostingStatus;

public record AccountOperationResponse(String transactionReference, PostingStatus status, String failureReason) {

}
