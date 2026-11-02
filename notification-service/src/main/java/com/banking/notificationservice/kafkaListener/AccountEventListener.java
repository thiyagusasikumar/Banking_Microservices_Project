package com.banking.notificationservice.kafkaListener;



import com.banking.notificationservice.dto.AccountEvent;
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
    public void consume(String message) throws JsonProcessingException {
        log.info("Received account event: {}", message);

        AccountEvent accountEvent = objectMapper.readValue(message, AccountEvent.class);

        if(accountEvent ==null || accountEvent.eventType()==null){
            log.info("Account event is null or eventType is null");
            return;
        }
        String eventType = accountEvent.eventType().toUpperCase(java.util.Locale.ROOT);
        if(!switch (eventType) {
            case "ACCOUNT_CREATED","ACCOUNT_BLOCKED" -> true;
            default -> false;
        }){
            log.info("Ignoring unsupported event type: {}", eventType);
            return;
        }
        notificationService.createAccountEvent(accountEvent);

    }
}
