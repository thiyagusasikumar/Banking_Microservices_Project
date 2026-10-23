package com.banking.notificationservice.service.Imp;


import com.banking.notificationservice.dto.NotificationResponse;
import com.banking.notificationservice.entity.Notification;
import com.banking.notificationservice.exception.ResourceNotFoundException;
import com.banking.notificationservice.repository.NotificationRepository;
import com.banking.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.config.ConfigDataResourceNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    public static final Set<String> SORT_FIELDS=Set.of("read","createdAt");
    private final NotificationRepository notificationRepository;
    @Override
    public Page<NotificationResponse> searchNotification(String query, int page, int size, String sortBy, String direction,Long recipientId) {
       if(page<0){
           throw new IllegalArgumentException("page size can't be less than 0");
       }
       if(size<1 || size>100){
           throw new IllegalArgumentException("size size can't be less than 1");
       }
       if(sortBy!=null && !SORT_FIELDS.contains(sortBy)){
           throw new IllegalArgumentException("sort field must in required format");
       }
        Sort.Direction sortDirection;
       try{
           sortDirection=Sort.Direction.fromString(direction);
       }
       catch(IllegalArgumentException e){
           throw new IllegalArgumentException("sort direction can't be less than 1");
       }
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection,sortBy));

       Page<Notification> notificationPage = notificationRepository.searchNotification(recipientId,query==null || query.isBlank()?null:query.trim(),pageable);

        return notificationPage.map(notification ->
                new NotificationResponse(
                        notification.getId(),
                        notification.getNotificationType(),
                        notification.getTitle(),
                        notification.getMessage(),
                        notification.isRead(),
                        notification.getCreatedAt()
                )
        );
    }

    @Override
    public void markAsRead(Long id, Long recipientId) {
        if(id==null || recipientId==null){
            throw new IllegalArgumentException("id or recipientId can't be null");
        }
       Notification notification= notificationRepository.findByIdAndRecipientId(id,recipientId).orElseThrow(()->new ResourceNotFoundException("Notification not found"));
       if(!notification.isRead()){
          notification.setRead(true);
           notification.setReadAt(LocalDateTime.now());
       }

       notificationRepository.save(notification);
    }
}
