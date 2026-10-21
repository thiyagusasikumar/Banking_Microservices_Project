package com.banking.authservice.dto;


import com.banking.authservice.constants.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AdminUserResponse {
    private Long id;
    private String userName;
    private String email;
    private UserStatus status;

}
