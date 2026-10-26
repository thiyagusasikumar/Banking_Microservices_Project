package com.banking.accountservice.entity;

import com.banking.accountservice.constant.AccountStatus;
import com.banking.accountservice.constant.AccountType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Customer ID is required")
    @Positive(message = "Customer ID must be positive")
    @Column(nullable = false)
    private Long customerId;

    @Column(nullable = false, unique = true, length = 50)
    private String accountNumber;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Account type is required")
    @Column(nullable = false)
    private AccountType accountType;

    @NotNull
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Pattern(regexp = "INR", message = "Only INR currency is supported")
    @NotBlank(message = "Currency is required")
    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @NotNull
    @Column(nullable = false, length = 30)
    private AccountStatus accountStatus;

    @Version
    private Long version;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;
}
