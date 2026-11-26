package com.banking.ledgerservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LedgerEntryNotFoundException extends RuntimeException {

    public LedgerEntryNotFoundException(Long entryId) {
        super("Ledger entry not found with id: " + entryId);
    }
}
