package com.banking.authservice.controller;



import com.banking.authservice.Service.KeycloakUserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/test/keycloak")
public class KeycloakTestController {

    private final KeycloakUserService keycloakUserService;

    public KeycloakTestController(KeycloakUserService keycloakUserService) {
        this.keycloakUserService = keycloakUserService;
    }

    @GetMapping("/users/count")
    public String getUserCount() {

        int count = keycloakUserService.getUserCount();

        return "Keycloak users: " + count;
    }
}
