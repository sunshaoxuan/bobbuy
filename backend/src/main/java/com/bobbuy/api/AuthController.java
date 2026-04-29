package com.bobbuy.api;

import com.bobbuy.api.response.ApiResponse;
import com.bobbuy.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public ApiResponse<AuthService.SessionResult> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.success(authService.login(request.username(), request.password(), clientFingerprint(httpServletRequest)));
    }

    @PostMapping("/refresh")
    public ApiResponse<AuthService.SessionResult> refresh(@Valid @RequestBody RefreshRequest request, HttpServletRequest httpServletRequest) {
        return ApiResponse.success(authService.refresh(request.refreshToken(), clientFingerprint(httpServletRequest)));
    }

    @PostMapping("/logout")
    public ApiResponse<AuthService.LogoutResult> logout(@RequestBody(required = false) LogoutRequest request) {
        return ApiResponse.success(authService.logout(request == null ? null : request.refreshToken()));
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

    public record RefreshRequest(@NotBlank String refreshToken) {
    }

    public record LogoutRequest(String refreshToken) {
    }
}
