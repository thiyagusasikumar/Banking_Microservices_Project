package com.banking.authservice.Service;


import com.banking.authservice.constants.RegistrationAttemptStatus;
import com.banking.authservice.constants.RegistrationType;
import com.banking.authservice.constants.Roles;
import com.banking.authservice.entity.RegistrationAttempt;
import com.banking.authservice.repository.RegistrationAttemptRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RegistrationAttemptService {
    private final RegistrationAttemptRepository registrationAttemptRepository;

    public RegistrationAttempt startRegistrationAttempt(
            String userName,
            RegistrationType registrationType,
            Roles registrationRole
    ) {
        RegistrationAttempt registrationAttempt = new RegistrationAttempt();
        registrationAttempt.setAttemptId(UUID.randomUUID().toString());
        registrationAttempt.setUserName(userName);
        registrationAttempt.setRegistrationType(registrationType);
        registrationAttempt.setRegistrationRole(registrationRole);
        registrationAttempt.setRegistrationStatus(RegistrationAttemptStatus.STARTED);
        return registrationAttemptRepository.save(registrationAttempt);
    }

    public Optional<RegistrationAttempt> findIncompleteAttempt(
            String userName,
            RegistrationType registrationType
    ) {
        return registrationAttemptRepository
                .findFirstByUserNameAndRegistrationTypeAndRegistrationStatusInOrderByCreatedAtAsc(
                        userName,
                        registrationType,
                        List.of(
                                RegistrationAttemptStatus.STARTED,
                                RegistrationAttemptStatus.IDENTITY_CREATED,
                                RegistrationAttemptStatus.LOCAL_USER_CREATED
                        )
                );
    }

    public void updateStatus(String attemptId, RegistrationAttemptStatus registrationAttemptStatus) {
        RegistrationAttempt attempt = registrationAttemptRepository.findByAttemptId(attemptId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Registration attempt not found: " + attemptId
                ));
        attempt.setRegistrationStatus(registrationAttemptStatus);
        registrationAttemptRepository.save(attempt);
    }

    public void updateKeycloakUserId(
            String attemptId,
            String keycloakUserId) {

        RegistrationAttempt attempt =
                registrationAttemptRepository.findByAttemptId(attemptId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Registration attempt not found: " + attemptId
                                )
                        );

        attempt.setKeycloakUserId(keycloakUserId);

        attempt.setRegistrationStatus(
                RegistrationAttemptStatus.IDENTITY_CREATED
        );

        registrationAttemptRepository.save(attempt);
    }

    public void updateLocalUserId(String attemptId, Long localUserId) {

        RegistrationAttempt attempt =
                registrationAttemptRepository.findByAttemptId(attemptId)
                        .orElseThrow(() ->
                                new EntityNotFoundException(
                                        "Registration attempt not found: " + attemptId
                                )
                        );

        attempt.setLocalUserId(localUserId);

        attempt.setRegistrationStatus(
                RegistrationAttemptStatus.LOCAL_USER_CREATED
        );

        registrationAttemptRepository.save(attempt);
    }

}
