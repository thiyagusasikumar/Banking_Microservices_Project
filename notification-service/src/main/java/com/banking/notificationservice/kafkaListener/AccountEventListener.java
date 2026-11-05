package com.banking.notificationservice.kafkaListener;



import com.banking.notificationservice.dto.AccountEvent;
import com.banking.notificationservice.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@RequiredArgsConstructor
@Slf4j
@Component
public class AccountEventListener {
    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;


    @KafkaListener(topics = "account-events")
    public void consume(String message) throws JsonProcessingException {
        AccountEvent accountEvent = objectMapper.readValue(message, AccountEvent.class);

        if (accountEvent == null || accountEvent.eventType() == null) {
            log.warn("Ignoring account event without an event type");
            return;
        }
        String eventType = accountEvent.eventType().trim().toUpperCase(java.util.Locale.ROOT);
        if (!switch (eventType) {
            case "ACCOUNT_CREATED", "ACCOUNT_BLOCKED" -> true;
            default -> false;
        }) {
            log.debug("Ignoring unsupported account event type {} for event {}",
                    eventType, accountEvent.eventId());
            return;
        }
        log.info("Received account event id={} type={}", accountEvent.eventId(), eventType);
        notificationService.createAccountEvent(accountEvent);
    }
}
