package com.banking.authservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(AbstractHttpConfigurer::disable)

                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/actuator/health",
                                "/actuator/info","/test/keycloak/**","/auth/login","/auth/public/register","/auth/refresh"
                        ).permitAll()
                        .requestMatchers("/auth/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated()
                )

                .oauth2ResourceServer(oauth2 ->
                        oauth2.jwt(Customizer.withDefaults())
                );
        return http.build();
    }
//    @Bean
//    Converter<Jwt, ? extends AbstractAuthenticationToken> jwtAuthenticationConverter() {
//        JwtGrantedAuthoritiesConverter scopes = new JwtGrantedAuthoritiesConverter();
//
//        return jwt -> {
//            Collection<GrantedAuthority> authorities = new ArrayList<>();
//            Collection<GrantedAuthority> scopeAuthorities = scopes.convert(jwt);
//            if (scopeAuthorities != null) {
//                authorities.addAll(scopeAuthorities);
//            }
//
//            Object realmAccess = jwt.getClaims().get("realm_access");
//            if (realmAccess instanceof Map<?, ?> claims
//                    && claims.get("roles") instanceof Collection<?> roles) {
//                for (Object role : roles) {
//                    if (role instanceof String name) {
//                        authorities.add(new SimpleGrantedAuthority("ROLE_" + name));
//                    }
//                }
//            }
//
//            return new JwtAuthenticationToken(jwt, authorities);
//        };
//    }
}
