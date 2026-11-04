package com.banking.authservice.config;

import com.banking.authservice.constants.UserStatus;
import com.banking.authservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.stereotype.Component;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomJwtAuthenticationConverter
        implements Converter<Jwt, AbstractAuthenticationToken> {

    private final UserRepository userRepository;

    @Override
    public AbstractAuthenticationToken convert(Jwt jwt) {

        boolean isActiveUser = userRepository
                .findByKeycloakUserId(jwt.getSubject())
                .map(user -> user.getStatus() == UserStatus.ACTIVE)
                .orElse(false);

        if (!isActiveUser) {
            throw new BadCredentialsException(
                    "User is inactive or not registered"
            );
        }

        Collection<GrantedAuthority> authorities =
                new ArrayList<>();

        JwtGrantedAuthoritiesConverter scopes =
                new JwtGrantedAuthoritiesConverter();

        Collection<GrantedAuthority> scopeAuthorities =
                scopes.convert(jwt);

        if (scopeAuthorities != null) {
            authorities.addAll(scopeAuthorities);
        }

        Object realmAccess = jwt.getClaims().get("realm_access");

        if (realmAccess instanceof Map<?, ?> claims
                && claims.get("roles") instanceof Collection<?> roles) {

            for (Object role : roles) {
                if (role instanceof String name) {
                    authorities.add(
                            new SimpleGrantedAuthority("ROLE_" + name)
                    );
                }
            }
        }

        return new JwtAuthenticationToken(jwt, authorities);
    }
}
