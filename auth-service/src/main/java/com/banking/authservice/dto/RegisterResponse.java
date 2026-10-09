package com.banking.authservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class RegisterResponse {

    private Long userId;
    private String keycloakUserId;
    private String username;
    private String email;
    private String message;
}