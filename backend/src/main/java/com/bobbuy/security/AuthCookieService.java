package com.bobbuy.security;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {
    private static final int CSRF_TOKEN_BYTES = 32;
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();

    private final String refreshCookieName;
    private final String refreshCookiePath;
    private final boolean secureCookie;
    private final String sameSite;
    private final String csrfCookieName;
    private final String csrfCookiePath;
    private final String csrfHeaderName;
    private final SecureRandom secureRandom = new SecureRandom();

    public AuthCookieService(
        @Value("${bobbuy.security.refresh-cookie.name:bobbuy_refresh_token}") String refreshCookieName,
        @Value("${bobbuy.security.refresh-cookie.path:/api/auth}") String refreshCookiePath,
        @Value("${bobbuy.security.refresh-cookie.secure:false}") boolean secureCookie,
        @Value("${bobbuy.security.refresh-cookie.same-site:Lax}") String sameSite,
        @Value("${bobbuy.security.csrf.cookie-name:bobbuy_csrf_token}") String csrfCookieName,
        @Value("${bobbuy.security.csrf.cookie-path:/}") String csrfCookiePath,
        @Value("${bobbuy.security.csrf.header-name:X-BOBBUY-CSRF-TOKEN}") String csrfHeaderName
    ) {
        this.refreshCookieName = refreshCookieName;
        this.refreshCookiePath = refreshCookiePath;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
        this.csrfCookieName = csrfCookieName;
        this.csrfCookiePath = csrfCookiePath;
        this.csrfHeaderName = csrfHeaderName;
    }

    public String resolveRefreshToken(HttpServletRequest request) {
        return resolveCookieValue(request, refreshCookieName).orElse(null);
    }

    public String resolveCsrfCookieToken(HttpServletRequest request) {
        return resolveCookieValue(request, csrfCookieName).orElse(null);
    }

    public String resolveCsrfHeaderToken(HttpServletRequest request) {
        return normalize(request.getHeader(csrfHeaderName)).orElse(null);
    }

    public boolean isCsrfValid(HttpServletRequest request) {
        String cookieToken = resolveCsrfCookieToken(request);
        String headerToken = resolveCsrfHeaderToken(request);
        return cookieToken != null && cookieToken.equals(headerToken);
    }

    public void writeRefreshAndCsrfCookies(HttpServletResponse response, String refreshToken, Instant refreshExpiresAt) {
        writeRefreshAndCsrfCookies(response, refreshToken, refreshExpiresAt, null);
    }

    public void writeRefreshAndCsrfCookies(HttpServletResponse response, String refreshToken, Instant refreshExpiresAt, String csrfToken) {
        long maxAgeSeconds = maxAgeSeconds(refreshExpiresAt);
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie(refreshToken, maxAgeSeconds).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCsrfCookie(resolveCsrfTokenForWrite(csrfToken), maxAgeSeconds).toString());
    }

    public void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, buildRefreshCookie("", 0).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, buildCsrfCookie("", 0).toString());
    }

    public String getRefreshCookieName() {
        return refreshCookieName;
    }

    public String getCsrfCookieName() {
        return csrfCookieName;
    }

    public String getCsrfHeaderName() {
        return csrfHeaderName;
    }

    public String getCsrfCookiePath() {
        return csrfCookiePath;
    }

    public boolean isSecureCookie() {
        return secureCookie;
    }

    private ResponseCookie buildRefreshCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(refreshCookieName, value)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path(refreshCookiePath)
            .maxAge(maxAgeSeconds)
            .build();
    }

    private ResponseCookie buildCsrfCookie(String value, long maxAgeSeconds) {
        return ResponseCookie.from(csrfCookieName, value)
            .httpOnly(false)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path(csrfCookiePath)
            .maxAge(maxAgeSeconds)
            .build();
    }

    private long maxAgeSeconds(Instant expiresAt) {
        if (expiresAt == null) {
            return 0L;
        }
        return Math.max(0L, expiresAt.getEpochSecond() - Instant.now().getEpochSecond());
    }

    private String generateCsrfToken() {
        byte[] tokenBytes = new byte[CSRF_TOKEN_BYTES];
        secureRandom.nextBytes(tokenBytes);
        return URL_ENCODER.encodeToString(tokenBytes);
    }

    private String resolveCsrfTokenForWrite(String csrfToken) {
        return normalize(csrfToken).orElseGet(this::generateCsrfToken);
    }

    private Optional<String> resolveCookieValue(HttpServletRequest request, String cookieName) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null || cookieName == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
            .filter(cookie -> cookieName.equals(cookie.getName()))
            .map(Cookie::getValue)
            .map(this::normalize)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .findFirst();
    }

    private Optional<String> normalize(String value) {
        if (value == null) {
            return Optional.empty();
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? Optional.empty() : Optional.of(normalized);
    }
}
