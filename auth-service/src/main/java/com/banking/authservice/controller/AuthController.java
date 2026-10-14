package com.banking.authservice.controller;

import com.banking.authservice.Service.AuthService;
import com.banking.authservice.Service.KeycloakUserService;
import com.banking.authservice.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final KeycloakUserService keycloakUserService;

    public AuthController(AuthService authService, KeycloakUserService keycloakUserService) {
        this.authService = authService;
        this.keycloakUserService = keycloakUserService;
    }

    @PostMapping("/public/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @PostMapping("/admin/employees")
    public ResponseEntity<RegisterResponse> createEmployee(@Valid @RequestBody RegisterEmployeeRequest request){
       return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerEmployee(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse loginResponse=authService.login(loginRequest);
        return ResponseEntity.ok(loginResponse);
    }
    @PostMapping("/refresh")
    public ResponseEntity<Map<String,Object>> refresh(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        return ResponseEntity.ok(
                keycloakUserService.refreshToken(refreshTokenRequest.getToken()));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody RefreshTokenRequest refreshTokenRequest) {
        authService.logout(refreshTokenRequest.getToken());
        return ResponseEntity.noContent().build();
    }
    @PatchMapping("/me/update")
    public ResponseEntity<UserUpdateResponse> userUpdate(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody UserUpdateRequest updateRequest) {
        UserUpdateResponse response=authService.userUpdate(jwt.getSubject(),updateRequest);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/me")
    public CurrentUserResponse getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
             String keycloakId=jwt.getSubject();
        return authService.getCurrentUser(keycloakId);
    }
}