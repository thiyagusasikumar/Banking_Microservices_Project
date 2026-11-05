package com.banking.accountservice.entity;

import com.banking.accountservice.constant.OperationType;
import com.banking.accountservice.constant.PostingStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
   import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "account_postings")
@Getter
@Setter
public class AccountPosting {
    @Id
    @Column(name = "transaction_reference", nullable = false)
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OperationType type;

    private Long sourceAccount;
    private Long destinationAccount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostingStatus status;

    private String failureReason;

}
