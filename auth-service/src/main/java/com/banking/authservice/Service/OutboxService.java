package com.banking.authservice.Service;

import com.banking.authservice.constants.OutboxStatus;
import com.banking.authservice.dto.AuthEvent;
import com.banking.authservice.entity.OutboxEvent;
import com.banking.authservice.entity.User;
import com.banking.authservice.repository.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OutboxService {

    private final OutboxEventRepository outBoxEventRepository;
    private final ObjectMapper objectMapper;

    public void saveUserRegisteredEvent(User user) {

        String eventId = UUID.randomUUID().toString();

        AuthEvent event = new AuthEvent(
                eventId,
            "REGISTRATION_COMPLETED",
                user.getId(),
                LocalDateTime.now()
        );

        try {
            String payload =
                    objectMapper.writeValueAsString(event);

           OutboxEvent outbox = new OutboxEvent();

            outbox.setEventId(eventId);
            outbox.setAggregationType("USER");
            outbox.setAggregateId(user.getId().toString());
            outbox.setEventType("REGISTRATION_COMPLETED");
            outbox.setPayLoad(payload);
            outbox.setStatus(OutboxStatus.PENDING);
            outbox.setRetryCount(0);
            outbox.setCreatedAt(LocalDateTime.now());
            outbox.setNextRetryAt(LocalDateTime.now());

            outBoxEventRepository.save(outbox);

        } catch (JsonProcessingException e) {
            throw new RuntimeException(
                    "Failed to serialize user event", e
            );
        }
    }
}

