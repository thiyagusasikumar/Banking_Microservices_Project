package com.banking.accountservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/accounts").authenticated()
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/accounts/me",
                                "/api/v1/accounts/me/customer-id",
                                "/api/v1/accounts/by-number/*",
                                "/api/v1/accounts/*",
                                "/api/v1/accounts/*/balance").authenticated()
//                        .requestMatchers("/api/v1/accounts/operations/**").hasRole("TRANSACTION_SERVICE")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/accounts",
                                "/api/v1/accounts/customer/*",
                                "/api/v1/accounts/*/status-history").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/accounts/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/accounts/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/accounts/*/freeze",
                                "/api/v1/accounts/*/unfreeze",
                                "/api/v1/accounts/*/close").hasRole("ADMIN")
                        .anyRequest().denyAll())
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt ->
                        jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Collection<GrantedAuthority> scopes = scopeConverter.convert(jwt);
            if (scopes != null) {
                authorities.addAll(scopes);
            }

            Object realmAccess = jwt.getClaims().get("realm_access");
            if (realmAccess instanceof Map<?, ?> claims
                    && claims.get("roles") instanceof Collection<?> roles) {
                for (Object role : roles) {
                    if (role instanceof String name) {
                        authorities.add(new SimpleGrantedAuthority("ROLE_" + name));
                    }
                }
            }
            return authorities;
        });
        return converter;
    }
}
