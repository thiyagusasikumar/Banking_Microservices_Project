package com.banking.authservice.exception;

public class AccountNotActiveException extends RuntimeException {
    public AccountNotActiveException() {
        super("Account is not active");
    }
}
