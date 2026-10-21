package com.banking.ledgerservice.service;

import com.banking.ledgerservice.dto.LedgerEntryResponse;
import com.banking.ledgerservice.dto.LedgerPostingRequest;
import com.banking.ledgerservice.dto.LedgerPostingResponse;



public interface LedgerService {

    LedgerPostingResponse postEntries(LedgerPostingRequest request);

    LedgerEntryResponse getEntryById(Long entryId);

}
