package com.manatandas.backend.security;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Puts the JWT in an httpOnly cookie instead of the JSON response body.
 * This means client-side JS (App.vue, useAuth.ts, or any injected XSS
 * script) can never read the token directly -- the browser just attaches
 * it automatically on requests to this API. The trade-off: the frontend
 * can no longer manually set "Authorization: Bearer <token>", so every
 * fetch() call that needs auth must instead pass { credentials: 'include' }
 * so the browser knows to send the cookie cross-origin.
 */
@Component
public class AuthCookieService {

    private final String cookieName;
    private final boolean secure;
    private final long maxAgeMs;

    public AuthCookieService(
        @Value("${app.cookie.name}") String cookieName,
        @Value("${app.cookie.secure}") boolean secure,
        @Value("${jwt.expiration-ms}") long maxAgeMs
    ) {
        this.cookieName = cookieName;
        this.secure = secure;
        this.maxAgeMs = maxAgeMs;
    }

    public void setAuthCookie(HttpServletResponse response, String token) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, token)
            .httpOnly(true)
            .secure(secure)
            .path("/")
            // Lax, not Strict: still blocks the cookie on genuine cross-site
            // requests (the actual CSRF threat), but doesn't break normal
            // same-site fetches the way Strict occasionally can. This is
            // also the whole reason CSRF protection can stay disabled in
            // SecurityConfig -- a cross-site attacker's forged request
            // simply won't have this cookie attached at all.
            .sameSite("Lax")
            .maxAge(Duration.ofMillis(maxAgeMs))
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearAuthCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(secure)
            .path("/")
            .sameSite("Lax")
            .maxAge(Duration.ZERO)
            .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public String getCookieName() {
        return cookieName;
    }
}
