package com.banking.notificationservice.service.Imp;

import com.banking.notificationservice.dto.NotificationResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NotificationSseService {
    private final Map<Long, Set<SseEmitter>> sseEmitters = new ConcurrentHashMap<>();

    public SseEmitter subscribe(Long customerId) {
        SseEmitter emitter = new SseEmitter(0L);
        sseEmitters.compute(customerId, (id, emitters) -> {
            Set<SseEmitter> customerEmitters =
                    emitters == null ? ConcurrentHashMap.newKeySet() : emitters;
            customerEmitters.add(emitter);
            return customerEmitters;
        });
        emitter.onCompletion(() -> removeEmitter(customerId, emitter));
        emitter.onTimeout(() -> {
            removeEmitter(customerId, emitter);
            emitter.complete();
        });
        emitter.onError(error -> removeEmitter(customerId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("SSE connection established"));
        } catch (IOException | IllegalStateException exception) {
            removeEmitter(customerId, emitter);
            emitter.completeWithError(exception);
        }
        return emitter;
    }

    public void sendToCustomer(Long customerId, NotificationResponse notification) {
        Set<SseEmitter> customerEmitters = sseEmitters.get(customerId);
        if (customerEmitters == null) {
            return;
        }

        for (SseEmitter emitter : customerEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("notification")
                        .data(notification));
            } catch (IOException | IllegalStateException exception) {
                removeEmitter(customerId, emitter);
                emitter.complete();
            }
        }
    }

    private void removeEmitter(Long customerId, SseEmitter emitter) {
        sseEmitters.computeIfPresent(customerId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
