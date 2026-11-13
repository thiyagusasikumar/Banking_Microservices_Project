package com.banking.accountservice.dto;


import com.banking.accountservice.constant.EventType;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountEvent {

    private String eventId;

    private EventType eventType;

    private Long customerId;

    private Long accountId;

    private String accountNumber;

    private String accountType;

    private Instant occurredAt;
}