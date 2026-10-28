package com.banking.authservice.dto;


import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CurrentUserResponse {

    private Long id;

    private String keycloakUserId;

    private String userName;

    private String email;

}
