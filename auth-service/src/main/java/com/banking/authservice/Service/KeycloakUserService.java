package com.banking.authservice.Service;

import com.banking.authservice.constants.EmployeeRoles;
import com.banking.authservice.constants.Roles;
import com.banking.authservice.dto.RegisterEmployeeRequest;
import com.banking.authservice.dto.RegisterRequest;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
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

        UserRepresentation user = new UserRepresentation();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        CredentialRepresentation password =
                new CredentialRepresentation();

        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(request.getPassword());
        password.setTemporary(false);

        user.setCredentials(List.of(password));

        RoleRepresentation role = keycloak
                .realm(realm)
                .roles()
                .get(Roles.CUSTOMER.name())
                .toRepresentation();

        Response response = keycloak
                .realm(realm)
                .users()
                .create(user);

        try {
            if (response.getStatus()
                    != Response.Status.CREATED.getStatusCode()) {

                throw new RuntimeException(
                        "Failed to create user in Keycloak. Status: "
                                + response.getStatus()
                );
            }

            String location = response.getHeaderString("Location");

            if (location == null) {
                throw new RuntimeException(
                        "Keycloak user ID was not returned"
                );
            }

            String keycloakUserId =
                    location.substring(location.lastIndexOf("/") + 1);

            // Assign CUSTOMER role to the created user
            keycloak.realm(realm)
                    .users()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .add(List.of(role));

            return keycloakUserId;

        } finally {
            response.close();
        }
    }

    public String createEmployee(RegisterEmployeeRequest request, EmployeeRoles newRole) {

        UserRepresentation user = new UserRepresentation();

        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);

        CredentialRepresentation password =
                new CredentialRepresentation();

        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(request.getPassword());
        password.setTemporary(false);

        user.setCredentials(List.of(password));


        RoleRepresentation role = keycloak
                .realm(realm)
                .roles()
                .get(String.valueOf(newRole))
                .toRepresentation();

        Response response = keycloak
                .realm(realm)
                .users()
                .create(user);

        try {
            if (response.getStatus()
                    != Response.Status.CREATED.getStatusCode()) {

                throw new RuntimeException(
                        "Failed to create user in Keycloak. Status: "
                                + response.getStatus()
                );
            }

            String location = response.getHeaderString("Location");

            if (location == null) {
                throw new RuntimeException(
                        "Keycloak user ID was not returned"
                );
            }

            String keycloakUserId =
                    location.substring(location.lastIndexOf("/") + 1);

            keycloak.realm(realm)
                    .users()
                    .get(keycloakUserId)
                    .roles()
                    .realmLevel()
                    .add(List.of(role));

            return keycloakUserId;

        } finally {
            response.close();
        }
    }
    public Map<String, Object> refreshToken(String refreshToken) {

        if (refreshToken == null || refreshToken.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Refresh token is required");
        }

        String url = serverUrl + "/realms/" + realm
                + "/protocol/openid-connect/token";

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

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
}

