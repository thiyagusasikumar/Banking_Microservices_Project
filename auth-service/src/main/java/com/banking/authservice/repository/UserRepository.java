package com.banking.authservice.repository;

import com.banking.authservice.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Integer> {
    Optional<User> findByKeycloakUserId(String keycloakUserId);

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);
}
