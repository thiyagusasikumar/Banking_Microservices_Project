package com.banking.notificationservice.cotroller;


import com.banking.notificationservice.dto.NotificationResponse;
import com.banking.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/getAll")
    public ResponseEntity<Page<NotificationResponse>> getALlNotifications(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String query, @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "asc") String direction){

        String customerId = jwt.getClaimAsString("customer_id");
        Long recipientId = Long.valueOf(customerId);
       return  ResponseEntity.ok(notificationService.searchNotification(query,page,size,sortBy,direction,recipientId));
    }
    @PatchMapping("/markAsRead/{id}")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal Jwt jwt,@PathVariable Long id){
        String customerId = jwt.getClaimAsString("customer_id");
        Long recipientId = Long.valueOf(customerId);
        notificationService.markAsRead(id,recipientId);
        return ResponseEntity.noContent().build();
    }

}
