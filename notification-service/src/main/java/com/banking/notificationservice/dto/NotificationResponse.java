package com.banking.notificationservice.dto;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    private Long id;

    private String notificationType;

    private String title;

    private String message;

    private LocalDateTime createdAt;

    private boolean read;
}
