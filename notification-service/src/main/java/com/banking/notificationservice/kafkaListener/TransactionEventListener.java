package com.banking.notificationservice.kafkaListener;


import com.banking.notificationservice.dto.TransactionCompletedEvent;
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

        TransactionCompletedEvent transactionCompletedEvent=objectMapper.readValue(message, TransactionCompletedEvent.class);

        notificationService.createFromTransactionEvent(transactionCompletedEvent);
    }
}
