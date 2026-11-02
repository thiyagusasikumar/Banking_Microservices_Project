package com.banking.accountservice.dto.kyc;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SubmitKycRequest {
    @NotBlank
    @Size(max = 40)
    private String documentType;

    @NotBlank
    @Size(max = 500)
    private String documentReference;
}
