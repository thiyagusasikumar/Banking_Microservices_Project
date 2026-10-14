package com.banking.authservice.Service;

import com.banking.authservice.constants.EmployeeRoles;
import com.banking.authservice.constants.Roles;
import com.banking.authservice.dto.RegisterEmployeeRequest;
import com.banking.authservice.dto.RegisterRequest;
import com.banking.authservice.dto.UserUpdateRequest;
import jakarta.ws.rs.core.Response;
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
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    public int getUserCount() {
        return keycloak
                .realm(realm)
                .users()
                .count();
    }


    public String createUser(RegisterRequest request) {
        return createUser(getUser(request), Roles.CUSTOMER.name());
    }

    public String createEmployee(RegisterEmployeeRequest request, EmployeeRoles newRole) {
        return createUser(getUser(request), newRole.name());
    }

    private UserRepresentation getUser(RegisterRequest request) {
        return buildUser(request.getUsername(), request.getEmail(), request.getFirstName(), request.getLastName(), request.getPassword());
    }

    private UserRepresentation getUser(RegisterEmployeeRequest request) {
        return buildUser(request.getUsername(), request.getEmail(), request.getFirstName(), request.getLastName(), request.getPassword());
    }

    private UserRepresentation buildUser(
            String username,
            String email,
            String firstName,
            String lastName,
            String rawPassword) {
        UserRepresentation user = new UserRepresentation();
        user.setUsername(username);
        user.setEmail(email);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEnabled(true);

        CredentialRepresentation password = new CredentialRepresentation();
        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(rawPassword);
        password.setTemporary(false);
        user.setCredentials(List.of(password));
        return user;
    }

    private String createUser(UserRepresentation user, String roleName) {
        RealmResource realmResource = keycloak.realm(realm);
        RoleRepresentation role = realmResource.roles()
                .get(roleName)
                .toRepresentation();

        try (Response response = realmResource.users().create(user)) {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new RuntimeException(
                        "Failed to create user in Keycloak. Status: " + response.getStatus());
            }

            String location = response.getHeaderString("Location");
            if (location == null) {
                throw new RuntimeException("Keycloak user ID was not returned");
            }

            String userId = location.substring(location.lastIndexOf("/") + 1);
            realmResource.users()
                    .get(userId)
                    .roles()
                    .realmLevel()
                    .add(List.of(role));
            return userId;
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
            if (currentEmail == null || !request.getEmail().equalsIgnoreCase(currentEmail)) {
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

        } catch (RestClientResponseException ex) {
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
}
