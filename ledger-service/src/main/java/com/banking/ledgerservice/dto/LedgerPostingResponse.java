package com.banking.ledgerservice.dto;

import java.util.List;
import java.util.UUID;

public record LedgerPostingResponse(UUID transactionId, List<LedgerEntryResponse> entries) {
}
