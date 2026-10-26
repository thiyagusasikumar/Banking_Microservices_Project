package com.banking.notificationservice.mapper;


import com.banking.notificationservice.dto.NotificationResponse;
import com.banking.notificationservice.entity.Notification;
import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;

@Mapper(componentModel = "spring")
public interface NotificationMapper {
    NotificationResponse toNotificationResponse(Notification notification);
}
