package com.banking.accountservice.entity;

import com.banking.accountservice.constant.AccountStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_status_history")
@Getter
@Setter
@NoArgsConstructor
public class AccountStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AccountStatus newStatus;

    @Column(nullable = false, length = 500)
    private String reason;

    @Column(nullable = false, length = 255)
    private String changedBy;

    @Column(nullable = false)
    private LocalDateTime changedAt;
}
