package com.banking.notificationservice.kafkaListener;

import com.banking.notificationservice.dto.AuthEvent;
import com.banking.notificationservice.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Locale;

@Component
@RequiredArgsConstructor
@Slf4j
public class AuthEventListener {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "auth-events")
    public void consume(String message) throws JsonProcessingException {
        AuthEvent event = objectMapper.readValue(message, AuthEvent.class);

        if (event == null || event.eventType() == null) {
            log.warn("Ignoring auth event without an event type");
            return;
        }
        String eventType = event.eventType().trim().toUpperCase(Locale.ROOT);
        if (!switch (eventType) {
            case "REGISTRATION", "REGISTRATION_COMPLETED", "SECURITY_ALERT" -> true;
            default -> false;
        }) {
            log.warn("Ignoring unsupported auth event type {} for event {}", eventType, event.eventId());
            return;
        }

        log.info("Received auth event id={} type={}", event.eventId(), eventType);
        notificationService.createAuthEvent(event);
    }
}
