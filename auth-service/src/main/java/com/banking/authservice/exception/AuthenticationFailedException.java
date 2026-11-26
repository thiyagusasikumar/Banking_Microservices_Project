package com.banking.authservice.exception;

import com.banking.authservice.constants.ErrorCode;
import lombok.Getter;


@Getter
public class AuthenticationFailedException extends RuntimeException {
    private final ErrorCode errorCode;

    public AuthenticationFailedException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }
}


