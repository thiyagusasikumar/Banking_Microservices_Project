package com.banking.authservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Username Required")
    private String username;

    @NotBlank(message="Password must not blank")
    private String password;
}
