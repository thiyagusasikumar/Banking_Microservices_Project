package com.banking.authservice.exception;

public class KeyCloakCreationException extends RuntimeException{
    public KeyCloakCreationException(String message) {
        super(message);
    }

    public KeyCloakCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
