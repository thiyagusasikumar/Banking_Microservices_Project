package com.banking.ledgerservice.exception;

public class LedgerEntryNotFoundException extends RuntimeException {

    public LedgerEntryNotFoundException(Long entryId) {
        super("Ledger entry not found with id: " + entryId);
    }
}
