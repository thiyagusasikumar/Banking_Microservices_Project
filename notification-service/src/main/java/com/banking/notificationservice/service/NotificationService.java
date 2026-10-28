package com.banking.notificationservice.service;


import com.banking.notificationservice.dto.NotificationResponse;
import com.banking.notificationservice.dto.TransactionCompletedEvent;
import org.springframework.data.domain.Page;
import org.springframework.security.oauth2.jwt.Jwt;


public interface NotificationService {
    Page<NotificationResponse> searchNotification(String query,int page,int size,String sortBy,String direction,Jwt jwt);

    void markAsRead(Long id, Jwt jwt);

    NotificationResponse getNotificationById(Long id, Jwt jwt);

    int markAllAsRead(Jwt jwt);

    void createFromTransactionEvent(TransactionCompletedEvent transactionCompletedEvent);

    Long requireCustomerId(Jwt jwt);

    long getUnreadCount(Jwt jwt);
}
