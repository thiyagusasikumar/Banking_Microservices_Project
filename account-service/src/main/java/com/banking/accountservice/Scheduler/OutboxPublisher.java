package com.banking.accountservice.Scheduler;

import com.banking.accountservice.constant.OutboxStatus;
import com.banking.accountservice.dto.AccountEvent;
import com.banking.accountservice.entity.AccountOutbox;
import com.banking.accountservice.repository.OutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
@RequiredArgsConstructor
public class OutboxPublisher {
    private static final int MAX_RETRIES = 5;
    private static final long INITIAL_RETRY_DELAY_SECONDS = 60;
    private static final long MAX_RETRY_DELAY_SECONDS = 3600;

    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, AccountEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;


    @Value("${app.kafka.topics.account-events}")
    private String accountEventTopic;

    @Scheduled(fixedDelay = 5000)
    public void publishPendingEvents() {

        List<AccountOutbox> events =
                outboxRepository.lockNextPendingBatch(Instant.now());

        for (AccountOutbox event : events) {
            try {
                AccountEvent payload = objectMapper.readValue(
                        event.getPayload(),
                        AccountEvent.class
                );

                kafkaTemplate.send(
                        accountEventTopic,
                        event.getEventId(),
                        payload
                ).get();

                markPublished(event);

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(
                        "Interrupted while publishing auth event", e
                );

            } catch (ExecutionException | JacksonException e) {
                recordFailure(event, e);
            }
        }
    }

    private void markPublished(AccountOutbox event) {
        event.setStatus(OutboxStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
        event.setLastError(null);

        outboxRepository.save(event);
    }

    private void recordFailure(AccountOutbox event, Exception error) {

        int retryCount = event.getRetryCount() + 1;

        event.setRetryCount(retryCount);
        event.setLastError(error.getMessage());

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
