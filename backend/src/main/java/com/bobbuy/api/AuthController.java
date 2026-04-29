package com.bobbuy.api;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.security.AuthCookieService;
import com.bobbuy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;
    private final AuthCookieService authCookieService;

    public AuthController(AuthService authService, AuthCookieService authCookieService) {
        this.authService = authService;
        this.authCookieService = authCookieService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.SessionResult> login(
        @Valid @org.springframework.web.bind.annotation.RequestBody LoginRequest request,
        HttpServletRequest httpServletRequest,
        HttpServletResponse httpServletResponse
    ) {
        AuthService.SessionResult sessionResult = authService.login(request.username(), request.password(), clientFingerprint(httpServletRequest));
        authCookieService.writeRefreshAndCsrfCookies(httpServletResponse, sessionResult.refreshToken(), sessionResult.refreshTokenExpiresAt());
        return ApiResponse.success(sessionResult);
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthService.SessionResult> refresh(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String refreshToken = authCookieService.resolveRefreshToken(httpServletRequest);
        if (refreshToken != null && !authCookieService.isCsrfValid(httpServletRequest)) {
            authCookieService.clearAuthCookies(httpServletResponse);
            throw new ApiException(ErrorCode.FORBIDDEN, "error.auth.invalid_csrf_token");
        }
        try {
            AuthService.SessionResult sessionResult = authService.refresh(refreshToken, clientFingerprint(httpServletRequest));
            authCookieService.writeRefreshAndCsrfCookies(httpServletResponse, sessionResult.refreshToken(), sessionResult.refreshTokenExpiresAt());
            return ApiResponse.success(sessionResult);
        } catch (RuntimeException ex) {
            authCookieService.clearAuthCookies(httpServletResponse);
            throw ex;
        }
    }

    @PostMapping("/logout")
    public ApiResponse<AuthService.LogoutResult> logout(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) {
        String refreshToken = authCookieService.resolveRefreshToken(httpServletRequest);
        if (refreshToken != null && !authCookieService.isCsrfValid(httpServletRequest)) {
            authCookieService.clearAuthCookies(httpServletResponse);
            throw new ApiException(ErrorCode.FORBIDDEN, "error.auth.invalid_csrf_token");
        }
        try {
            return ApiResponse.success(authService.logout(refreshToken));
        } finally {
            authCookieService.clearAuthCookies(httpServletResponse);
        }
    }

    @GetMapping("/me")
    public ApiResponse<AuthService.UserProfile> currentUser(Authentication authentication) {
        return ApiResponse.success(authService.currentUser(authentication));
    }

    private String clientFingerprint(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {
    }

}
