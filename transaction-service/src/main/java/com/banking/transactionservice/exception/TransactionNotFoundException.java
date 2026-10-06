package com.banking.transactionservice.exception;

public class TransactionNotFoundException extends RuntimeException {
    public TransactionNotFoundException(String reference) { super("Transaction not found: " + reference); }
}
