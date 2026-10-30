package com.banking.notificationservice.kafkaListener;


import com.banking.notificationservice.dto.TransactionEvent;
import com.banking.notificationservice.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@RequiredArgsConstructor
@Component
@Slf4j
public class TransactionEventListener {
    private final ObjectMapper objectMapper;
    private final NotificationService notificationService;

    @KafkaListener(topics = "transaction-events")
    public void consume(String message) throws JsonProcessingException {
        log.info("Received transaction event: {}", message);

        TransactionEvent transactionEvent = objectMapper.readValue(message, TransactionEvent.class);
        if (transactionEvent == null || transactionEvent.eventType() == null) {
            log.warn("Ignoring transaction event without an event type");
            return;
        }

        String eventType = transactionEvent.eventType().trim().toUpperCase(java.util.Locale.ROOT);
        if (!switch (eventType) {
            case "TRANSACTION_COMPLETED", "DEPOSIT", "AMOUNT_DEPOSITED",
                    "WITHDRAWAL", "AMOUNT_WITHDRAWN", "MONEY_RECEIVED" -> true;
            default -> false;
        }) {
            log.debug("Ignoring unsupported transaction event type: {}", transactionEvent.eventType());
            return;
        }

        notificationService.createFromTransactionEvent(transactionEvent);
    }
}
