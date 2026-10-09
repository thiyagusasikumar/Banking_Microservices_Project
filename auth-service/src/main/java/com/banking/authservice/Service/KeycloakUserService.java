package com.banking.authservice.Service;

import com.banking.authservice.dto.RegisterRequest;
import jakarta.ws.rs.core.Response;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KeycloakUserService {

    private final Keycloak keycloak;

    public KeycloakUserService(Keycloak keycloak) {
        this.keycloak = keycloak;
    }

    public int getUserCount() {
        return keycloak
                .realm("banking-service")
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

        CredentialRepresentation password = new CredentialRepresentation();

        password.setType(CredentialRepresentation.PASSWORD);
        password.setValue(request.getPassword());
        password.setTemporary(false);

        user.setCredentials(List.of(password));

        Response response = keycloak
                .realm("banking-service")
                .users()
                .create(user);

        try {
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {

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
            return location.substring(location.lastIndexOf("/") + 1);

        } finally {
            response.close();
        }
    }
}

