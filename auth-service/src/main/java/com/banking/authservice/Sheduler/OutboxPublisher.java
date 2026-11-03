package com.banking.authservice.Sheduler;

import com.banking.authservice.constants.OutboxStatus;
import com.banking.authservice.dto.AuthEvent;
import com.banking.authservice.entity.OutboxEvent;
import com.banking.authservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int MAX_RETRIES = 5;
    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, AuthEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.auth-events}")
    private String authEventsTopic;

    @Scheduled(fixedDelay = 5000)
    @Transactional
    public void publishPendingEvents() {
        List<OutboxEvent> events =
                outboxRepository.lockNextPendingBatch(LocalDateTime.now());

        for (OutboxEvent event : events) {
            try {
                AuthEvent payload = objectMapper.readValue(event.getPayLoad(), AuthEvent.class);
                kafkaTemplate.send(authEventsTopic, event.getAggregateId(), payload).get();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("Interrupted while publishing auth event", e);
            } catch (ExecutionException | JsonProcessingException | KafkaException e) {
                recordFailure(event, e);
                continue;
            }

            event.setStatus(OutboxStatus.PUBLISHED);
            event.setPublishedAt(LocalDateTime.now());
            event.setLastErrorMessage(null);
            outboxRepository.save(event);
        }
    }

    private void recordFailure(OutboxEvent event, Exception error) {
        int retryCount = event.getRetryCount() + 1;
        event.setRetryCount(retryCount);
        event.setLastErrorMessage(error.getMessage());

        if (retryCount >= MAX_RETRIES) {
            event.setStatus(OutboxStatus.FAILED);
            return;
        }
        outboxRepository.save(event);
    }
}
