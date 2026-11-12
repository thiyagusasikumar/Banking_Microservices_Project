
package com.banking.notificationservice.scheduler;

import com.banking.notificationservice.constants.EmailStatus;
import com.banking.notificationservice.entity.EmailOutbox;
import com.banking.notificationservice.repository.EmailOutboxRepository;
import com.banking.notificationservice.service.Imp.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EmailOutboxScheduler {
    private static final int MAX_RETRY_COUNT = 5;
    private static final long INITIAL_DELAY_SECONDS = 60;
    private static final long MAX_DELAY_SECONDS = 3600;

    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailNotificationService emailNotificationService;

    @Scheduled(fixedDelay = 5000)
    public void scheduleEmailOutboxScheduler() {
        List<EmailOutbox> emails =
                emailOutboxRepository
                        .findTop100ByEmailStatusAndNextRetryAtLessThanEqualOrderByCreatedAtAsc(
                                EmailStatus.PENDING, Instant.now());

        for (EmailOutbox email : emails) {
            try {
                switch (email.getEmailType()) {
                    case REGISTRATION_COMPLETED ->
                            emailNotificationService.sendRegistrationEmail(
                                    email.getRecipientEmail(), email.getName());
                    case SECURITY_ALERT ->
                            emailNotificationService.sendSecurityAlertEmail(
                                    email.getRecipientEmail(), email.getName());
                }
                markSent(email);
            } catch (MailException exception) {
                recordFailure(email, exception);
            }
        }
    }

    private void markSent(EmailOutbox email) {
        email.setEmailStatus(EmailStatus.SENT);
        email.setSentAt(Instant.now());
        email.setErrorMessage(null);
        emailOutboxRepository.save(email);
    }

    private void recordFailure(EmailOutbox email, MailException exception) {
        int retryCount = email.getRetryCount() + 1;
        email.setRetryCount(retryCount);
        email.setErrorMessage(exception.getMessage());

        if (retryCount >= MAX_RETRY_COUNT) {
            email.setEmailStatus(EmailStatus.FAILED);
        } else {
            long delaySeconds = Math.min(
                    INITIAL_DELAY_SECONDS * (1L << (retryCount - 1)),
                    MAX_DELAY_SECONDS);
            email.setNextRetryAt(Instant.now().plusSeconds(delaySeconds));
        }

        emailOutboxRepository.save(email);
    }
}