package com.banking.accountservice.dto.kyc;

import com.banking.accountservice.constant.KycStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReviewKycRequest {
    @NotNull
    private KycStatus decision;

    @Size(max = 1000)
    private String note;
}
