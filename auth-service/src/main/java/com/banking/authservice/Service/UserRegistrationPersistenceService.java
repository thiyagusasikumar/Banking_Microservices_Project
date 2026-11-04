package com.banking.authservice.Service;

import com.banking.authservice.entity.User;
import com.banking.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRegistrationPersistenceService {

    private final UserRepository userRepository;
    private final OutboxService outboxService;

    @Transactional
    public User saveUserAndOutboxEvent(User user) {
        User savedUser = userRepository.save(user);
        outboxService.saveUserRegisteredEvent(savedUser);
        return savedUser;
    }
}
