package com.banking.ledgerservice.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ValidatedPosting(UUID transactionId, Long debitAccountId, Long creditAccountId,
                                BigDecimal amount, String currency) {
    }