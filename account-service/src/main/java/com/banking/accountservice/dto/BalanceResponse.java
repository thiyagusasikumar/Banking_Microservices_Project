package com.banking.accountservice.dto;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BalanceResponse {

    private Long accountId;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
}