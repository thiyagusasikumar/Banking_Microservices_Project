package com.banking.authservice.util;


import com.banking.authservice.constants.ApplicationConstants;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class CookieUtil {
    public void addRefreshTokenToCookie(HttpServletResponse httpServletResponse, String refreshToken,long refreshExpiresIn) {
        ResponseCookie cookie = ResponseCookie.from(ApplicationConstants.REFRESH_TOKEN_COOKIE,refreshToken)
                .httpOnly(true)
                .secure(true)
                .path(ApplicationConstants.COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(refreshExpiresIn))
                .build();

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshTokenCookie(HttpServletResponse httpServletResponse) {
        ResponseCookie cookie =ResponseCookie
                .from(ApplicationConstants.REFRESH_TOKEN_COOKIE,"")
                .httpOnly(true)
                .secure(true)
                .path(ApplicationConstants.COOKIE_PATH)
                .sameSite("Lax")
                .maxAge(Duration.ofSeconds(0))
                .build();

        httpServletResponse.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

    }
}
