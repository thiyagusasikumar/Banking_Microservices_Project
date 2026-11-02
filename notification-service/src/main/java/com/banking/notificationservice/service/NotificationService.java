package com.banking.notificationservice.service;


import com.banking.notificationservice.dto.AccountEvent;
import com.banking.notificationservice.dto.AuthEvent;
import com.banking.notificationservice.dto.NotificationResponse;
import com.banking.notificationservice.dto.TransactionEvent;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.jwt.Jwt;


public interface NotificationService {
    Page<NotificationResponse> searchNotification(String query, int page, int size, String sortBy, String direction, Jwt jwt);

    NotificationResponse getNotificationById(Long id, Jwt jwt);

    void markAsRead(Long id, Jwt jwt);

    int markAllAsRead(Jwt jwt);

    long getUnreadCount(Jwt jwt);

    void createFromTransactionEvent(TransactionEvent transactionEvent);

    void createAccountEvent(AccountEvent accountEvent);

    void createAuthEvent(AuthEvent authEvent);

    Long requireCustomerId(Jwt jwt);
}
