package com.banking.authservice.Sheduler;

import com.banking.authservice.constants.OutboxStatus;
import com.banking.authservice.dto.AuthEvent;
import com.banking.authservice.entity.OutboxEvent;
import com.banking.authservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {

    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_SECONDS = 60;
    private static final long MAX_RETRY_DELAY_SECONDS = 3600;

    private final OutboxEventRepository outboxRepository;
    private final KafkaTemplate<String, AuthEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    @Value("${app.kafka.topics.auth-events}")
    private String authEventsTopic;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<OutboxEvent> events =
                outboxRepository.lockNextPendingBatch(Instant.now());

        for (OutboxEvent event : events) {
            try {
                AuthEvent payload = objectMapper.readValue(
                        event.getPayLoad(),
                        AuthEvent.class
                );

                kafkaTemplate.send(
                        authEventsTopic,
                        event.getEventId(),
                        payload
                ).get();

                markPublished(event);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Interrupted while publishing auth event", e
                );

            } catch (ExecutionException | JsonProcessingException e) {
                recordFailure(event, e);
            }
        }
    }

    private void markPublished(OutboxEvent event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        event.setLastErrorMessage(null);

        outboxRepository.save(event);
    }

    private void recordFailure(OutboxEvent event, Exception error) {

        int retryCount = event.getRetryCount() + 1;

        event.setRetryCount(retryCount);
        event.setLastErrorMessage(error.getMessage());

        if (retryCount >= MAX_RETRIES) {
            event.setStatus(OutboxStatus.FAILED);
        } else {
            long delaySeconds = Math.min(
                    INITIAL_RETRY_DELAY_SECONDS * (1L << (retryCount - 1)),
                    MAX_RETRY_DELAY_SECONDS
            );

            event.setNextRetryAt(
                    Instant.now().plusSeconds(delaySeconds)
            );
        }

        outboxRepository.save(event);
    }
}