package com.banking.transactionservice.dto;

import java.math.BigDecimal;

public record AccountOperationRequest(String transactionReference, String type,
                                      String sourceAccount, String destinationAccount,
                                      BigDecimal amount, String currency) {}
