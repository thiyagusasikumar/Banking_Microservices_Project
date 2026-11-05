package com.banking.accountservice.dto;

import com.banking.accountservice.constant.PostingStatus;

public record AccountOperationResponse(String transactionReference, PostingStatus status, String failureReason) {
}
