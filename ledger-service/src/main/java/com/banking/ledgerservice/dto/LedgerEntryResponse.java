package com.banking.ledgerservice.dto;

import com.banking.ledgerservice.constant.EntryType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryResponse(
        Long id,
        UUID transactionId,
        Long accountId,
        EntryType entryType,
        BigDecimal amount,
        String currency,
        Instant createdAt
) {
}
