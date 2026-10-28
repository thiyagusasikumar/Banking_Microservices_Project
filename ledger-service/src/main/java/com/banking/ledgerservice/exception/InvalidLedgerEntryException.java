package com.banking.ledgerservice.exception;

public class InvalidLedgerEntryException extends RuntimeException {

    public InvalidLedgerEntryException(String message) {
        super(message);
    }
}
