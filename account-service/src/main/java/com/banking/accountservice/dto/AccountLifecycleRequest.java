package com.banking.accountservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountLifecycleRequest {

    @NotBlank(message = "Status change reason is required")
    @Size(max = 500, message = "Reason must not exceed 500 characters")
    private String reason;
}
