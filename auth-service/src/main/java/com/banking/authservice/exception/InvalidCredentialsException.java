package com.banking.authservice.exception;

import com.banking.authservice.constants.ErrorCode;
import lombok.Getter;


@Getter
public class InvalidCredentialsException extends RuntimeException{
    private final ErrorCode errorCode;
    public InvalidCredentialsException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode=errorCode;
    }
}
