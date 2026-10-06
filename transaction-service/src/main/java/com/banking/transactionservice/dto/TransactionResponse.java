package com.banking.transactionservice.dto;

import com.banking.transactionservice.entity.TransactionRecord;
import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(String reference, TransactionRecord.Type type,
                                  TransactionRecord.Status status, String sourceAccount,
                                  String destinationAccount, BigDecimal amount, String currency,
                                  String failureReason, Instant createdAt, Instant updatedAt) {}
