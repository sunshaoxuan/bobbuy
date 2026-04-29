package com.bobbuy.security;

import com.bobbuy.model.User;
import com.bobbuy.repository.UserRepository;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

@Service
public class BearerTokenAuthenticationService {
    private final JwtTokenService jwtTokenService;
    private final UserRepository userRepository;

    public BearerTokenAuthenticationService(JwtTokenService jwtTokenService, UserRepository userRepository) {
        this.jwtTokenService = jwtTokenService;
        this.userRepository = userRepository;
    }

    public Authentication authenticate(String token) {
        JwtTokenService.VerifiedToken verifiedToken = jwtTokenService.verify(token);
        User user = userRepository.findById(verifiedToken.userId())
            .filter(User::isEnabled)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        BobbuyAuthenticatedUser principal =
            new BobbuyAuthenticatedUser(user.getId(), user.getUsername(), user.getName(), user.getRole());
        return new UsernamePasswordAuthenticationToken(
            principal,
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
