package com.banking.authservice.controller;

import com.banking.authservice.Service.AuthService;
import com.banking.authservice.dto.RegisterRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {


    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(
            @Valid @RequestBody RegisterRequest request) {

        authService.register(request);

        return ResponseEntity.ok("Registration request received");
    }
    @GetMapping("/me")
    public String getCurrentUser(@AuthenticationPrincipal Jwt jwt) {

        return "Keycloak User ID: " + jwt.getSubject();
    }
}