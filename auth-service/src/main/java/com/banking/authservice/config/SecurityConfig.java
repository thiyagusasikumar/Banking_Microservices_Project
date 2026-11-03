package com.banking.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

import org.springframework.security.web.SecurityFilterChain;


@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final CustomJwtAuthenticationConverter customJwtAuthenticationConverter;

    public SecurityConfig(
            CustomJwtAuthenticationConverter customJwtAuthenticationConverter) {
        this.customJwtAuthenticationConverter = customJwtAuthenticationConverter;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info",
                                "/auth/login",
                                "/auth/public/register",
                                "/auth/refresh"
                        ).permitAll()
                        .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(jwt ->
                                jwt.jwtAuthenticationConverter(
                                        customJwtAuthenticationConverter
                                )
                        )
                );

        return http.build();
    }
}
//    @Bean
//    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
//
//        JwtGrantedAuthoritiesConverter scopes =
//                new JwtGrantedAuthoritiesConverter();
//
//        return jwt -> {
//            boolean isActiveUser = userRepository
//                    .findByKeycloakUserId(jwt.getSubject())
//                    .map(user -> user.getStatus() == UserStatus.ACTIVE)
//                    .orElse(false);
//
//            if (!isActiveUser) {
//                throw new BadCredentialsException(
//                        "User is inactive or not registered"
//                );
//            }
//
//            Collection<GrantedAuthority> authorities =
//                    new ArrayList<>();
//
//            Collection<GrantedAuthority> scopeAuthorities =
//                    scopes.convert(jwt);
//
//            if (scopeAuthorities != null) {
//                authorities.addAll(scopeAuthorities);
//            }
//
//            Object realmAccess = jwt.getClaims().get("realm_access");
//
//            if (realmAccess instanceof Map<?, ?> claims
//                    && claims.get("roles") instanceof Collection<?> roles) {
//
//                for (Object role : roles) {
//                    if (role instanceof String name) {
//                        authorities.add(
//                                new SimpleGrantedAuthority("ROLE_" + name)
//                        );
//                    }
//                }
//            }
//
//            return new JwtAuthenticationToken(jwt, authorities);
//        };
//    }

