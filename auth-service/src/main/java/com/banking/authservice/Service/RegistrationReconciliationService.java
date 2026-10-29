package com.banking.authservice.Service;

import com.banking.authservice.constants.RegistrationAttemptStatus;
import com.banking.authservice.constants.Roles;
import com.banking.authservice.dto.RegisterResponse;
import com.banking.authservice.entity.RegistrationAttempt;
import com.banking.authservice.entity.User;
import com.banking.authservice.mapper.UserMapper;
import com.banking.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RegistrationReconciliationService {

    private final RegistrationAttemptService registrationAttemptService;
    private final KeycloakUserService keycloakUserService;
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    public RegisterResponse reconcileAttempt(RegistrationAttempt attempt) {

        Optional<UserRepresentation> existingUser =
                keycloakUserService.findUserByRegistrationAttempt(
                        attempt.getAttemptId()
                );

        if (existingUser.isEmpty()) {
            throw new IllegalStateException(
                    "No Keycloak user found for registration attempt: "
                            + attempt.getAttemptId()
            );
        }

        UserRepresentation keycloakUser = existingUser.get();
        String keycloakUserId = keycloakUser.getId();

        registrationAttemptService.updateKeycloakUserId(
                attempt.getAttemptId(),
                keycloakUserId
        );

        Roles role = resolveRegistrationRole(attempt, keycloakUserId);
        keycloakUserService.ensureRealmRole(keycloakUserId, role.name());

        User user = userRepository
                .findByKeycloakUserId(keycloakUserId)
                .orElseGet(() -> createLocalUserFromAttempt(
                        keycloakUser,
                        role
                ));

        registrationAttemptService.updateLocalUserId(
                attempt.getAttemptId(),
                user.getId()
        );

        keycloakUserService.updateCustomerIdAttribute(
                user.getId(),
                keycloakUserId
        );
        registrationAttemptService.updateStatus(
                attempt.getAttemptId(),
                RegistrationAttemptStatus.COMPLETED
        );

        return userMapper.toRegisterResponse(user);
    }


    private Roles resolveRegistrationRole(
            RegistrationAttempt attempt,
            String keycloakUserId
    ) {
        if (attempt.getRegistrationRole() != null) {
            return attempt.getRegistrationRole();
        }

        List<String> roleNames = keycloakUserService.getUserRealmRoles(keycloakUserId);
        return Arrays.stream(Roles.values())
                .filter(role -> roleNames.contains(role.name()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException("No matching application role found")
                );
    }

    private User createLocalUserFromAttempt(
            UserRepresentation keycloakUser,
            Roles role
    ) {

        User user = new User();

        user.setKeycloakUserId(keycloakUser.getId());
        user.setUserName(keycloakUser.getUsername());
        user.setEmail(keycloakUser.getEmail());
        user.setFirstName(keycloakUser.getFirstName());
        user.setLastName(keycloakUser.getLastName());

        Map<String, List<String>> attributes =
                keycloakUser.getAttributes();

        if (attributes != null && attributes.get("phoneNumber") != null
                && !attributes.get("phoneNumber").isEmpty()) {

            user.setPhoneNumber(
                    attributes.get("phoneNumber").get(0)
            );
        }
        user.setRole(role);

        return userRepository.save(user);
    }
}
