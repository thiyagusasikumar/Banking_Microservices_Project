package com.banking.accountservice.dto;

import com.banking.accountservice.constant.AccountStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountStatusHistoryResponse {

    private Long id;
    private Long accountId;
    private AccountStatus previousStatus;
    private AccountStatus newStatus;
    private String reason;
    private String changedBy;
    private LocalDateTime changedAt;
}