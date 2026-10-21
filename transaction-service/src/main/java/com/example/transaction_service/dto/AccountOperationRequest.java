package com.example.transaction_service.dto;

import com.example.transaction_service.constants.Type;

import java.math.BigDecimal;

public record AccountOperationRequest(String transactionReference, Type type, Long sourceAccount,
                                      Long destinationAccount, BigDecimal amount, String currency) {
}
