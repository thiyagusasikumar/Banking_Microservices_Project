package com.banking.ledgerservice.mapper;

import com.banking.ledgerservice.dto.LedgerEntryResponse;
import com.banking.ledgerservice.entity.LedgerEntry;
import org.springframework.stereotype.Component;

@Component
public class LedgerEntryMapper {

    public LedgerEntryResponse toResponse(LedgerEntry entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getTransactionId(),
                entry.getAccountId(),
                entry.getEntryType(),
                entry.getAmount(),
                entry.getCurrency(),
                entry.getCreatedAt()
        );
    }
}
