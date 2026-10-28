package com.banking.notificationservice.kafkaListener;


import com.banking.notificationservice.dto.AccountCreatedEvent;
import com.banking.notificationservice.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Slf4j
@Component
public class AccountEventListener {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "account-events")
    public void consume (String message) throws JsonProcessingException {
        log.info("Received account event: {}", message);

        AccountCreatedEvent accountCreatedEvent = objectMapper.readValue(message, AccountCreatedEvent.class);
    }
}
