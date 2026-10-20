package com.banking.authservice.config;

import com.banking.authservice.constants.UserStatus;
import com.banking.authservice.entity.User;
import com.banking.authservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    private final UserRepository userRepository = mock(UserRepository.class);
    private final SecurityConfig securityConfig = new SecurityConfig(userRepository);

    @Test
    void acceptsJwtForActiveUser() {
        User user = new User();
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByKeycloakUserId("user-123")).thenReturn(Optional.of(user));

        assertInstanceOf(JwtAuthenticationToken.class,
                securityConfig.jwtAuthenticationConverter().convert(jwt()));
    }

    @Test
    void rejectsJwtForInactiveUser() {
        User user = new User();
        user.setStatus(UserStatus.INACTIVE);
        when(userRepository.findByKeycloakUserId("user-123")).thenReturn(Optional.of(user));

        assertThrows(BadCredentialsException.class,
                () -> securityConfig.jwtAuthenticationConverter().convert(jwt()));
    }

    @Test
    void rejectsJwtWhenUserIsNotRegisteredLocally() {
        when(userRepository.findByKeycloakUserId("user-123")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class,
                () -> securityConfig.jwtAuthenticationConverter().convert(jwt()));
    }

    private Jwt jwt() {
        Instant now = Instant.now();
        return Jwt.withTokenValue("token")
                .header("alg", "RS256")
                .subject("user-123")
                .issuedAt(now)
                .expiresAt(now.plusSeconds(300))
                .build();
    }
}
