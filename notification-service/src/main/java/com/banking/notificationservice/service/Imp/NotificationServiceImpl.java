package com.banking.notificationservice.service.Imp;


import com.banking.notificationservice.constants.EmailStatus;
import com.banking.notificationservice.constants.EmailType;
import com.banking.notificationservice.constants.NotificationType;
import com.banking.notificationservice.dto.*;
import com.banking.notificationservice.entity.EmailOutbox;
import com.banking.notificationservice.entity.Notification;
import com.banking.notificationservice.exception.ForbiddenOperationException;
import com.banking.notificationservice.exception.ResourceNotFoundException;
import com.banking.notificationservice.mapper.NotificationMapper;
import com.banking.notificationservice.repository.EmailOutboxRepository;
import com.banking.notificationservice.repository.NotificationRepository;
import com.banking.notificationservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    public static final Set<String> SORT_FIELDS = Set.of("read", "createdAt");

    private static final Set<String> SORT_DIRECTIONS = Set.of("ASC", "DESC");

    private final NotificationRepository notificationRepository;
    private final NotificationMapper notificationMapper;
    private final NotificationSseService notificationSseService;
    private final EmailOutboxRepository emailOutboxRepository;


    @Override
    public Page<NotificationResponse> searchNotification(String query, int page, int size, String sortBy, String direction, Jwt jwt) {
        Long recipientId = requireCustomerId(jwt);
        if (page < 0) {
            throw new IllegalArgumentException("Page must be zero or greater.");
        }
        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100.");
        }
        if (sortBy == null || !SORT_FIELDS.contains(sortBy)) {
            throw new IllegalArgumentException("Sort field must be one of: read, createdAt.");
        }
        String normalizedDirection = direction == null ? "" : direction.toUpperCase(Locale.ROOT);
        if (!SORT_DIRECTIONS.contains(normalizedDirection)) {
            throw new IllegalArgumentException("Sort direction must be either asc or desc.");
        }
        Sort.Direction sortDirection = Sort.Direction.valueOf(normalizedDirection);
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortDirection, sortBy));

        Page<Notification> notificationPage = notificationRepository.searchNotification(recipientId, query == null || query.isBlank() ? null : query.trim(), pageable);

        return notificationPage.map(notificationMapper::toNotificationResponse);

    }

    @Override
    public void markAsRead(Long id, Jwt jwt) {
        requireValidNotificationId(id);
        Long recipientId = requireCustomerId(jwt);
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (!notification.isRead()) {
            notification.setRead(true);
            notification.setReadAt(Instant.now());
        }

        notificationRepository.save(notification);
    }

    @Override
    @Transactional
    public int markAllAsRead(Jwt jwt) {
        Long recipientId = requireCustomerId(jwt);
        return notificationRepository.markAllAsRead(recipientId, Instant.now());
    }

    @Override
    public NotificationResponse getNotificationById(Long id, Jwt jwt) {
        requireValidNotificationId(id);
        Long recipientId = requireCustomerId(jwt);
        Notification notification = notificationRepository.findByIdAndRecipientId(id, recipientId).orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        return notificationMapper.toNotificationResponse(notification);
    }


    @Override
    public void createFromTransactionEvent(TransactionEvent event) {
        if (event == null
                || event.eventId() == null
                || event.eventId().isBlank()
                || event.customerId() == null
                || event.transactionId() == null
                || event.eventType() == null
                || event.eventType().isBlank()) {
            throw new ForbiddenOperationException("Invalid Transaction Event");
        }
        if (!"COMPLETED".equalsIgnoreCase(event.status())) {
            return;
        }

        NotificationType type = setNotificationType(event.eventType());
        saveAndSendIfAbsent(getNotification(event, type));

        if (type == NotificationType.TRANSACTION_COMPLETED
                && event.receiverId() != null
                && !event.customerId().equals(event.receiverId())) {
            saveAndSendIfAbsent(createReceiverMessage(event));
        }
    }

    @Override
    public void createAccountEvent(AccountEvent event) {
        if (event == null
                || event.eventId() == null
                || event.eventId().isBlank()
                || event.customerId() == null
                || event.eventType() == null
                || event.eventType().isBlank()) {
            throw new ForbiddenOperationException("Invalid Account Event");
        }
        NotificationType type = setNotificationType(event.eventType());
        saveAndSendIfAbsent(getNotification(event, type));
    }

    @Override
    public void createAuthEvent(AuthEvent event) {
        if (event == null
                || event.eventId() == null
                || event.eventId().isBlank()
                || event.customerId() == null
                || event.eventType() == null
                || event.eventType().isBlank()
                || event.email() == null
                || event.email().isBlank()
                || event.customerName() == null
                || event.customerName().isBlank()) {
            throw new ForbiddenOperationException("Invalid Auth Event");
        }
        String eventType = event.eventType().trim().toUpperCase(Locale.ROOT);
        NotificationType type = setNotificationType(eventType);
        EmailType emailType = switch (eventType) {
            case "REGISTRATION", "REGISTRATION_COMPLETED" -> EmailType.REGISTRATION_COMPLETED;
            case "SECURITY_ALERT" -> EmailType.SECURITY_ALERT;
            default -> throw new IllegalArgumentException("Unsupported auth event type: " + eventType);
        };

        saveAndSendIfAbsent(getNotification(event, type));
        if (!emailOutboxRepository.existsByEventIdAndEmailType(event.eventId(), emailType)) {
            EmailOutbox emailOutbox = new EmailOutbox();
            emailOutbox.setEventId(event.eventId());
            emailOutbox.setEmailType(emailType);
            emailOutbox.setRecipientEmail(event.email());
            emailOutbox.setEmailStatus(EmailStatus.PENDING);
            emailOutbox.setRetryCount(0);
            emailOutbox.setNextRetryAt(Instant.now());
            emailOutbox.setName(event.customerName());
            emailOutboxRepository.save(emailOutbox);
        }
    }

    private Notification getNotification(TransactionEvent event, NotificationType notificationType) {
        Notification notification = new Notification();
        notification.setRecipientId(event.customerId());
        notification.setEventId(event.eventId());
        notification.setNotificationType(notificationType);
        notification.setTitle(switch (notificationType) {
            case AMOUNT_DEPOSITED -> "Deposit Completed";
            case AMOUNT_WITHDRAWN -> "Withdrawal Completed";
            case MONEY_RECEIVED -> "Money Received";
            default -> "Transaction Completed";
        });
        notification.setMessage(setNotificationMessage(event, notificationType));
        notification.setTransactionId(event.transactionId());
        notification.setCurrency(event.currency());
        notification.setAmount(event.amount());
        return notification;
    }

    private Notification getNotification(AccountEvent event, NotificationType notificationType) {
        Notification notification = new Notification();
        notification.setRecipientId(event.customerId());
        notification.setEventId(event.eventId());
        notification.setNotificationType(notificationType);
        notification.setTitle(switch (notificationType) {
            case ACCOUNT_CREATED -> "Account Created";
            case ACCOUNT_BLOCKED -> "Account Blocked";
            default -> "Transaction Completed";
        });
        notification.setAccountId(event.accountId());
        notification.setAccountType(event.accountType());
        notification.setOccurredAt(event.occurredAt());
        notification.setMessage(setNotificationMessage(event, notificationType));
        return notification;
    }

    private Notification getNotification(AuthEvent event, NotificationType notificationType) {
        Notification notification = new Notification();
        notification.setRecipientId(event.customerId());
        notification.setEventId(event.eventId());
        notification.setNotificationType(notificationType);
        notification.setRecipientName(event.customerName());
        notification.setRecipientEmail(event.email());
        notification.setOccurredAt(event.occurredAt());
        notification.setTitle(switch (notificationType)
        {
            case REGISTRATION_COMPLETED -> "Registration Completed";
            case SECURITY_ALERT ->  "Security Alert";
            default -> "Auth service Completed";
        });
        notification.setMessage(setNotificationMessage(event,notificationType));
        return notification;
    }
    public Notification createReceiverMessage(TransactionEvent event) {
        Notification notification = new Notification();
        notification.setRecipientId(event.receiverId());
        notification.setEventId(event.eventId());

        notification.setNotificationType(NotificationType.MONEY_RECEIVED);
        notification.setTitle("Money Received");
        notification.setMessage("Your transaction " + event.transactionId()
                + " for " + event.currency() + " "
                + event.amount() + " was received successfully.");
        notification.setCurrency(event.currency());
        notification.setAmount(event.amount());
        return notification;
    }

    public NotificationType setNotificationType(String eventType) {
        if (eventType == null || eventType.isBlank()) {
            throw new IllegalArgumentException("Transaction event type is required.");
        }
        return switch (eventType.trim().toUpperCase(Locale.ROOT)) {
            case "TRANSACTION_COMPLETED" -> NotificationType.TRANSACTION_COMPLETED;
            case "DEPOSIT", "AMOUNT_DEPOSITED" -> NotificationType.AMOUNT_DEPOSITED;
            case "WITHDRAWAL", "AMOUNT_WITHDRAWN" -> NotificationType.AMOUNT_WITHDRAWN;
            case "MONEY_RECEIVED" -> NotificationType.MONEY_RECEIVED;
            case "ACCOUNT_CREATED" -> NotificationType.ACCOUNT_CREATED;
            case "ACCOUNT_BLOCKED" -> NotificationType.ACCOUNT_BLOCKED;
            case "REGISTRATION", "REGISTRATION_COMPLETED" -> NotificationType.REGISTRATION_COMPLETED;
            case "SECURITY_ALERT" -> NotificationType.SECURITY_ALERT;
            default -> throw new IllegalArgumentException("Unsupported transaction event type: " + eventType);
        };
    }

    private boolean saveAndSendIfAbsent(Notification notification) {
        if (notificationRepository.existsByEventIdAndRecipientId(
                notification.getEventId(), notification.getRecipientId())) {
            return false;
        }

        Notification savedNotification;
        try {
            savedNotification = notificationRepository.saveAndFlush(notification);
        } catch (DataIntegrityViolationException exception) {
            if (notificationRepository.existsByEventIdAndRecipientId(
                    notification.getEventId(), notification.getRecipientId())) {
                return false;
            }
            throw exception;
        }

        notificationSseService.sendToCustomer(
                savedNotification.getRecipientId(),
                notificationMapper.toNotificationResponse(savedNotification)
        );
        return true;
    }

    private String setNotificationMessage(TransactionEvent event, NotificationType notificationType) {
        String result = "Your transaction " + event.transactionId()
                + " for " + event.currency() + " " + event.amount();
        return switch (notificationType) {
            case AMOUNT_DEPOSITED -> result + " was deposited successfully.";
            case AMOUNT_WITHDRAWN -> result + " was withdrawn successfully.";
            case MONEY_RECEIVED -> result + " was received successfully.";
            case ACCOUNT_CREATED -> "Your account was created successfully.";
            case ACCOUNT_BLOCKED -> "Your account has been blocked.";
            default -> result + " was completed successfully.";
        };
    }

    private String setNotificationMessage(AccountEvent event, NotificationType notificationType) {
        String maskedAccountNumber = maskAccountNumber(event.accountNumber());
        String message = "Your account ending in "
                + maskedAccountNumber;
        return switch (notificationType) {
            case ACCOUNT_CREATED -> message + " was created successfully.";
            case ACCOUNT_BLOCKED -> message + " was blocked successfully.";
            default -> " Account  event was completed successfully.";
        };
    }

    private String setNotificationMessage(AuthEvent event, NotificationType notificationType) {
        return switch (notificationType) {
            case REGISTRATION_COMPLETED -> "Registration was completed successfully.";
            case SECURITY_ALERT -> "An unauthorized login attempt was detected.";
            default -> "Auth event completed successfully.";
        };
    }


    @Override
    public Long requireCustomerId(Jwt jwt) {
        String value = jwt == null ? null : jwt.getClaimAsString("customer_id");

        if (value == null || value.isBlank()) {
            throw new ForbiddenOperationException("JWT is missing customer_id.");
        }

        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            throw new ForbiddenOperationException("JWT customer_id must be numeric.", e);
        }
    }

    @Override
    public long getUnreadCount(Jwt jwt) {
        Long recipientId = requireCustomerId(jwt);
        return notificationRepository.countByRecipientIdAndReadFalse(recipientId);
    }


    private void requireValidNotificationId(Long id) {
        if (id == null || id < 1) {
            throw new IllegalArgumentException("Notification ID must be a positive number.");
        }
    }

    private String maskAccountNumber(String accountNumber) {
        if (accountNumber == null || accountNumber.length() <= 4) {
            return "****";
        }

        return "****" + accountNumber.substring(accountNumber.length() - 4);
    }

}
