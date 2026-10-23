package com.banking.notificationservice.service;


import com.banking.notificationservice.dto.NotificationResponse;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;


public interface NotificationService {
    Page<NotificationResponse> searchNotification(String query,int page,int size,String sortBy,String direction,Long recipientId);

    void markAsRead(Long id,Long recipientId);
}
