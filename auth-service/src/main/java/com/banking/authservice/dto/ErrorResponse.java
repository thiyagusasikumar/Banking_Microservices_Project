package com.banking.authservice.dto;

import lombok.*;

import java.time.Instant;

@Data
@AllArgsConstructor
@Getter
@Setter
@RequiredArgsConstructor
public class ErrorResponse {
    private int status;
    private String code;
    private String message;
    private Instant timestamp;
}
