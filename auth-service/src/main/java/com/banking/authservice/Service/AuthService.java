package com.banking.authservice.Service;


import com.banking.authservice.constants.EmployeeRoles;
import com.banking.authservice.constants.Roles;
import com.banking.authservice.dto.*;
import com.banking.authservice.entity.User;
import com.banking.authservice.exception.InvalidCredentialsException;


import com.banking.authservice.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Locale;


@Service
public class AuthService {
    @Value("${keycloak.server-url}")
    private String serverUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.token-url}")
    private String tokenUrl;

    @Value("${keycloak.client-id}")
    private String clientId;

    @Value("${keycloak.client-secret}")
    private String clientSecret;

    private final UserRepository userRepository;
    private final KeycloakUserService keycloakUserService;
    private final RestClient restClient;

    public AuthService(UserRepository userRepository, KeycloakUserService keycloakUserService, RestClient restClient) {
        this.userRepository = userRepository;
        this.keycloakUserService = keycloakUserService;
        this.restClient = restClient;
    }

    public void register(RegisterRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        String keycloakUserId = keycloakUserService.createUser(request);

        User user = new User();

        user.setKeycloakUserId(keycloakUserId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Roles.CUSTOMER);
        userRepository.save(user);
    }
    public void registerEmployee(RegisterEmployeeRequest request) {

        if (userRepository.findByUsername(request.getUsername()).isPresent()) {
            throw new RuntimeException("Username already exists");
        }

        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already exists");
        }

        EmployeeRoles role;

        try {
            role = EmployeeRoles.valueOf(
                    request.getRole().trim().toUpperCase(Locale.ROOT)
            );
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new IllegalArgumentException("Invalid employee role");
        }

        String keycloakUserId = keycloakUserService.createEmployee(request,role);

        User user = new User();

        user.setKeycloakUserId(keycloakUserId);
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setRole(Roles.valueOf(role.name()));
        userRepository.save(user);
    }

    public LoginResponse login(LoginRequest loginRequest)  {
         MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
         formData.add("grant_type", "password");
         formData.add("client_id",clientId);
         formData.add("client_secret",clientSecret);
         formData.add("username", loginRequest.getUsername());
         formData.add("password", loginRequest.getPassword());
         try{
             KeycloakTokenResponse tokenResponse=restClient.post().uri(tokenUrl).
                     contentType(MediaType.APPLICATION_FORM_URLENCODED).
                     body(formData).
                     retrieve().
                     body(KeycloakTokenResponse.class);

             if(tokenResponse==null || tokenResponse.getAccessToken()==null){
                 throw new IllegalStateException("Invalid token response from Keycloak");
             }

             return new LoginResponse(
                     tokenResponse.getAccessToken(),
                     tokenResponse.getRefreshToken(),
                     tokenResponse.getTokenType(),
                     tokenResponse.getExpiresIn()
             );

         }
         catch (HttpClientErrorException.BadRequest
                | HttpClientErrorException.Unauthorized ex) {

             throw new InvalidCredentialsException(
                     "Invalid username or password");

         } catch (RestClientException ex) {

             throw new IllegalStateException(
                     "Unable to communicate with Keycloak", ex);
         }
    }
    public void logout(String token) {
        String logoutUrl = serverUrl
                + "/realms/" + realm
                + "/protocol/openid-connect/logout";

        MultiValueMap<String, String> form =
                new LinkedMultiValueMap<>();

        form.add("client_id", clientId);
        form.add("refresh_token", token);

        if (clientSecret != null && !clientSecret.isBlank()) {
            form.add("client_secret", clientSecret);
        }

        restClient.post()
                .uri(logoutUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .toBodilessEntity();
    }
    public CurrentUserResponse getCurrentUser(String keycloakUserId)
    {
        User user=userRepository.findByKeycloakUserId(keycloakUserId).orElseThrow(()->new UsernameNotFoundException("User not found"));
        return new CurrentUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getKeycloakUserId()
        );
    }
}

