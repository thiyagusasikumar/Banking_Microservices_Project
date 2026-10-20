package com.banking.ledgerservice.exception;

public class DuplicateLedgerEntryException extends RuntimeException {

    public DuplicateLedgerEntryException(String message) {
        super(message);
    }
}
