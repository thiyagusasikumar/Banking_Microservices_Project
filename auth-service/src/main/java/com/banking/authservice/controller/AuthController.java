package com.banking.authservice.controller;

import com.banking.authservice.Service.AuthService;
import com.banking.authservice.Service.KeycloakUserService;
import com.banking.authservice.constants.ErrorCode;
import com.banking.authservice.dto.*;
import com.banking.authservice.exception.AuthenticationFailedException;
import com.banking.authservice.util.CookieUtil;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;
    private final KeycloakUserService keycloakUserService;
    private final CookieUtil cookieUtil;

    public AuthController(AuthService authService, KeycloakUserService keycloakUserService, CookieUtil cookieUtil) {
        this.authService = authService;
        this.keycloakUserService = keycloakUserService;
        this.cookieUtil = cookieUtil;
    }

    @PostMapping("/public/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest,HttpServletResponse servletResponse) {
        LoginResult loginResult = authService.login(loginRequest);
         cookieUtil.addRefreshTokenToCookie(
                 servletResponse,loginResult.getRefreshToken(),loginResult.getRefreshExpiresIn()
         );
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(loginResult.getLoginResponse());
    }
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(
            @CookieValue(name ="refresh_token",required = false)String refreshToken, HttpServletResponse servletResponse) {
        if(refreshToken==null || refreshToken.isEmpty()){
            throw new AuthenticationFailedException(ErrorCode.REFRESH_TOKEN_INVALID,"Invalid refresh token");
        }
        LoginResult loginResult=authService.refreshToken(refreshToken);
        cookieUtil.addRefreshTokenToCookie(
                servletResponse,loginResult.getRefreshToken(),loginResult.getRefreshExpiresIn());

        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(loginResult.getLoginResponse());
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = "refresh_token",required = false)String refreshToken, HttpServletResponse servletResponse) {
        if(refreshToken==null || refreshToken.isEmpty()){
            throw new AuthenticationFailedException(ErrorCode.REFRESH_TOKEN_INVALID,"Invalid refresh token");
        }
        try{
            authService.logout(refreshToken);
        } finally {
            cookieUtil.clearRefreshTokenCookie(servletResponse);
        }
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
    @PostMapping("/deactivate")
    public ResponseEntity<String> deactivateUser(@AuthenticationPrincipal Jwt jwt) {
        String keycloakId=jwt.getSubject();
        authService.deactivateUser(keycloakId);
        return ResponseEntity.ok("deactivated");
    }
    @PostMapping("/forgot-password")
    public ResponseEntity<String> forgotPassword(
            @Valid @RequestBody ForgotPassword forgotPassword) {
        authService.forgotPassword(forgotPassword);
        return ResponseEntity.accepted().body(
                "If an account exists for that login, password reset email will be sent."
        );
    }
}