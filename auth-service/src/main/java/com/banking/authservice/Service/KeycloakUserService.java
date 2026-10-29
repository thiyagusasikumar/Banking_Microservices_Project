package com.banking.authservice.Service;

import com.banking.authservice.constants.EmployeeRoles;
import com.banking.authservice.constants.Roles;
import com.banking.authservice.dto.RegisterEmployeeRequest;
import com.banking.authservice.dto.RegisterRequest;
import com.banking.authservice.dto.UserUpdateRequest;
import com.banking.authservice.dto.KeycloakTokenResponse;
import com.banking.authservice.exception.AuthenticationFailedException;
import com.banking.authservice.exception.KeyCloakCreationException;
import com.banking.authservice.exception.DuplicateUserException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.WebApplicationException;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class KeycloakUserService {
    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    private final Keycloak keycloak;
    private  final RestClient restClient;

    public KeycloakUserService(Keycloak keycloak, RestClient restClient) {
        this.keycloak = keycloak;
        this.restClient = restClient;
    }

    public String createUser(RegisterRequest request,String attemptId) {
        UserRepresentation user = getUser(request);

        Map<String, List<String>> attributes =
                user.getAttributes() == null
                        ? new HashMap<>()
                        : new HashMap<>(user.getAttributes());

        attributes.put(
                "registration_attempt_id",
                List.of(attemptId)
        );

        user.setAttributes(attributes);
        return createUser(user, Roles.CUSTOMER.name());
    }

    public String createEmployee(RegisterEmployeeRequest request, EmployeeRoles newRole, String attemptId) {
        UserRepresentation user = getUser(request);

        Map<String, List<String>> attributes =
                user.getAttributes() == null
                        ? new HashMap<>()
                        : new HashMap<>(user.getAttributes());

        attributes.put(
                "registration_attempt_id",
                List.of(attemptId)
        );

        user.setAttributes(attributes);
        return createUser(user, newRole.name());
    }
    public Optional<UserRepresentation> findUserByRegistrationAttempt(
            String attemptId) {

        List<UserRepresentation> users = keycloak
                .realm(realm)
                .users()
                .searchByAttributes(
                        "registration_attempt_id:" + attemptId
                );

        if (users.isEmpty()) {
            return Optional.empty();
        }

        if (users.size() > 1) {
            throw new IllegalStateException(
                    "Multiple Keycloak users found for registration attempt"
            );
        }

        return Optional.of(users.get(0));
    }

    public void verifyCredentials(String userName, String password) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("username", userName);
        form.add("password", password);

        try {
            KeycloakTokenResponse tokenResponse = restClient.post()
                    .uri(serverUrl + "/realms/" + realm + "/protocol/openid-connect/token")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(KeycloakTokenResponse.class);

            if (tokenResponse == null || tokenResponse.getAccessToken() == null) {
                throw new AuthenticationFailedException("Invalid username or password");
            }
        } catch (HttpClientErrorException.BadRequest
                 | HttpClientErrorException.Unauthorized ex) {
            throw new AuthenticationFailedException("Invalid username or password");
        }
    }

    private UserRepresentation getUser(RegisterRequest request) {
        return buildUser(request.getUserName(), request.getEmail(), request.getFirstName(),
                request.getLastName(), request.getPassword(), request.getPhoneNumber());
    }

    private UserRepresentation getUser(RegisterEmployeeRequest request) {
        return buildUser(request.getUserName(), request.getEmail(), request.getFirstName(),
                request.getLastName(), request.getPassword(), request.getPhoneNumber());
    }

    private UserRepresentation buildUser(
            String username,
            String email,
            String firstName,
            String lastName,
            String rawPassword,
            String phoneNumber) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);
        if (phoneNumber != null) {
            user.setAttributes(Map.of("phoneNumber", List.of(phoneNumber)));
        }

        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(rawPassword);
        password.setTemporary(false);
        user.setCredentials(List.of(password));
        return user;
    }

    private String createUser(UserRepresentation user, String roleName) {
        try {
            RealmResource realmResource = keycloak.realm(realm);
            RoleRepresentation role = realmResource.roles()
                    .get(roleName)
                    .toRepresentation();

            try (Response response = realmResource.users().create(user)) {
                if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                    throw new DuplicateUserException("Username or email already exists");
                }
                if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                    throw new KeyCloakCreationException("Identity provider could not create user");
                }

                String location = response.getHeaderString("Location");
                if (location == null) {
                    throw new KeyCloakCreationException("Identity provider returned an invalid response");
                }

                String userId = location.substring(location.lastIndexOf("/") + 1);
                realmResource.users()
                        .get(userId)
                        .roles()
                        .realmLevel()
                        .add(List.of(role));
                return userId;
            }
        } catch (WebApplicationException ex) {
            if (ex.getResponse() != null
                    && ex.getResponse().getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new DuplicateUserException("Username or email already exists");
            }
            throw new KeyCloakCreationException("Identity provider could not create user", ex);
        } catch (ProcessingException ex) {
            throw new KeyCloakCreationException("Identity provider is unavailable", ex);
        }
    }
    public void updateProfile(String keycloakUserId, UserUpdateRequest request){
        UserResource userResource = keycloak
                .realm(realm)
                .users()
                .get(keycloakUserId);

        UserRepresentation representation =
                userResource.toRepresentation();

        if(request.getEmail()!=null){
            String currentEmail = representation.getEmail();
            representation.setEmail(request.getEmail());
            if (!request.getEmail().equalsIgnoreCase(currentEmail)) {
                representation.setEmailVerified(false);
            }
        }
        if(request.getFirstName()!=null){
            representation.setFirstName(request.getFirstName());
        }
        if(request.getLastName()!=null){
            representation.setLastName(request.getLastName());
        }
        if (request.getPhoneNumber() != null) {
            Map<String, List<String>> attributes = representation.getAttributes();
            if (attributes == null) {
                attributes = new HashMap<>();
                representation.setAttributes(attributes);
            }
            attributes.put("phoneNumber", List.of(request.getPhoneNumber()));
        }
        userResource.update(representation);
    }

    public Map<String, Object> refreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refresh token is required");
        }

        String url = serverUrl + "/realms/" + realm
                + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();

        form.add("grant_type", "refresh_token");
        form.add("client_id", clientId);
        form.add("refresh_token", refreshToken);

        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        try {
            return restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

        } catch (HttpClientErrorException.BadRequest
                 | HttpClientErrorException.Unauthorized ex) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh token is invalid or expired");
        }
    }
    public void updateCustomerIdAttribute(Long customerId,String keycloakUserId) {
        UserResource userResource = keycloak
                .realm(realm)
                .users()
                .get(keycloakUserId);

        UserRepresentation representation =
                userResource.toRepresentation();

        Map<String, List<String>> attributes = representation.getAttributes();
        if (attributes == null) {
            attributes = new HashMap<>();
        }
        attributes.put("customer_id", List.of(customerId.toString()));

        representation.setAttributes(attributes);

        userResource.update(representation);
    }
    public void deactivateUser(String keycloakUserId) {
        setUserEnabled(keycloakUserId, false);
    }

    public void activateUser(String keycloakUserId) {
        setUserEnabled(keycloakUserId, true);
    }

    private void setUserEnabled(String keycloakUserId, boolean enabled) {
        try {
            UserResource userResource = keycloak.realm(realm).users().get(keycloakUserId);
            UserRepresentation representation = userResource.toRepresentation();
            if (representation == null) {
                throw new UsernameNotFoundException("User not found");
            }
            representation.setEnabled(enabled);
            userResource.update(representation);
        } catch (WebApplicationException | ProcessingException ex) {
            throw new KeyCloakCreationException("Identity provider could not update user status", ex);
        }
    }
    public List<String> getUserRealmRoles(String keycloakUserId) {

        return keycloak.realm(realm)
                .users()
                .get(keycloakUserId)
                .roles()
                .realmLevel()
                .listAll()
                .stream()
                .map(RoleRepresentation::getName)
                .toList();
    }

    public void ensureRealmRole(String keycloakUserId, String roleName) {
        RealmResource realmResource = keycloak.realm(realm);
        UserResource userResource = realmResource.users().get(keycloakUserId);

        boolean alreadyAssigned = userResource.roles()
                .realmLevel()
                .listAll()
                .stream()
                .anyMatch(role -> roleName.equals(role.getName()));
        if (!alreadyAssigned) {
            RoleRepresentation role = realmResource.roles()
                    .get(roleName)
                    .toRepresentation();
            userResource.roles()
                    .realmLevel()
                    .add(List.of(role));
        }
    }
}
