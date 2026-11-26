package com.banking.authservice.dto;


import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class LoginResult {

    private LoginResponse loginResponse;

    private String refreshToken;

    private long refreshExpiresIn;
}
