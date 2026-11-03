package com.banking.notificationservice.controller;


import com.banking.notificationservice.dto.NotificationResponse;
import com.banking.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/notification")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping("/getAll")
    public ResponseEntity<Page<NotificationResponse>> getALlNotifications(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) String query, @RequestParam(defaultValue = "0") int page
            , @RequestParam(defaultValue = "10") int size, @RequestParam(defaultValue = "createdAt") String sortBy, @RequestParam(defaultValue = "asc") String direction) {
        return ResponseEntity.ok(notificationService.searchNotification(query, page, size, sortBy, direction, jwt));
    }

    @PatchMapping("/markAsRead/{id}")
    public ResponseEntity<Void> markAsRead(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        notificationService.markAsRead(id, jwt);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<NotificationResponse> getNotification(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return ResponseEntity.ok(notificationService.getNotificationById(id, jwt));
    }

    @PatchMapping("/markAllRead")
    public ResponseEntity<Map<String, Object>> markAllAsRead(@AuthenticationPrincipal Jwt jwt) {
        int updatedCount = notificationService.markAllAsRead(jwt);
        return ResponseEntity.ok(Map.of("message", "All notifications marked as read", "updatedCount", updatedCount));
    }

    @GetMapping("/getUnreadCount")
    public ResponseEntity<Long> getUnreadCount(@AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(notificationService.getUnreadCount(jwt));
    }

}
