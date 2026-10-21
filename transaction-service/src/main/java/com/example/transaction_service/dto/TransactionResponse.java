package com.example.transaction_service.dto;

import com.example.transaction_service.constants.Status;
import com.example.transaction_service.constants.Type;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(String reference, Type type,
                                  Status status, String sourceAccount,
                                  String destinationAccount, BigDecimal amount, String currency,
                                  String failureReason, Instant createdAt, Instant updatedAt) {
}