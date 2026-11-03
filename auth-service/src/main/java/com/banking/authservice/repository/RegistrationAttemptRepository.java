package com.banking.authservice.repository;

import com.banking.authservice.entity.RegistrationAttempt;
import com.banking.authservice.constants.RegistrationAttemptStatus;
import com.banking.authservice.constants.RegistrationType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;


@Repository
public interface RegistrationAttemptRepository extends JpaRepository<RegistrationAttempt, String> {
    Optional<RegistrationAttempt> findByAttemptId(String attemptId);

    Optional<RegistrationAttempt> findFirstByUserNameAndRegistrationTypeAndRegistrationStatusInOrderByCreatedAtAsc(
            String userName,
            RegistrationType registrationType,
            Collection<RegistrationAttemptStatus> statuses
    );

}
