package com.banking.authservice.entity;

import com.banking.authservice.constants.RegistrationAttemptStatus;
import com.banking.authservice.constants.RegistrationType;
import com.banking.authservice.constants.Roles;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "registration_attempts")
@Getter
@Setter
@NoArgsConstructor
public class RegistrationAttempt {

    @Id
    @Column(name = "attempt_id", length = 36,unique = true)
    private String attemptId;

    @Column(name = "user_name", nullable = false, length = 100)
    private String userName;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_type", length = 16)
    private RegistrationType registrationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_role", length = 20)
    private Roles registrationRole;

    @Column(name = "keycloak_user_id", length = 100)
    private String keycloakUserId;

    @Column(name = "local_user_id")
    private Long localUserId;

    @Enumerated(EnumType.STRING)
    @Column(name = "registration_status", nullable = false, length = 32)
    private RegistrationAttemptStatus registrationStatus;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
