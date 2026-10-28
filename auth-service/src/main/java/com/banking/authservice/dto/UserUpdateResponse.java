package com.banking.authservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserUpdateResponse {
    private String userName;

    private String email;

    private String firstName;

    private String lastName;

    private String phoneNumber;
}
