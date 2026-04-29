package com.bobbuy.service;

import com.bobbuy.api.response.ApiException;
import com.bobbuy.api.response.ErrorCode;
import com.bobbuy.model.User;
import com.bobbuy.repository.UserRepository;
import com.bobbuy.security.BobbuyAuthenticatedUser;
import com.bobbuy.security.JwtTokenService;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
    }

    public LoginResult login(String username, String password) {
        User user = userRepository.findByUsernameIgnoreCase(username == null ? "" : username.trim())
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.invalid_credentials"));
        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.user_disabled");
        }
        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.invalid_credentials");
        }
        return new LoginResult(jwtTokenService.createAccessToken(user), toUserProfile(user));
    }

    public UserProfile currentUser(Authentication authentication) {
        User user = resolveUser(authentication)
            .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.invalid_credentials"));
        if (!user.isEnabled()) {
            throw new ApiException(ErrorCode.UNAUTHORIZED, "error.auth.user_disabled");
        }
        return toUserProfile(user);
    }

    private Optional<User> resolveUser(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return Optional.empty();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof BobbuyAuthenticatedUser authenticatedUser && authenticatedUser.id() != null) {
            return userRepository.findById(authenticatedUser.id());
        }
        try {
            return userRepository.findById(Long.parseLong(authentication.getName()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private UserProfile toUserProfile(User user) {
        return new UserProfile(user.getId(), user.getUsername(), user.getName(), user.getRole().name());
    }

    public record LoginResult(String accessToken, UserProfile user) {
    }

    public record UserProfile(Long id, String username, String name, String role) {
    }
}
