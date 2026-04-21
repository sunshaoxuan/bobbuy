package com.bobbuy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

@Component
public class RoleInjectionFilter extends OncePerRequestFilter {
    public static final String ROLE_HEADER = "X-BOBBUY-ROLE";
    private final String defaultRole;

    public RoleInjectionFilter(@Value("${bobbuy.security.default-role:CUSTOMER}") String defaultRole) {
        this.defaultRole = defaultRole == null ? "CUSTOMER" : defaultRole.toUpperCase(Locale.ROOT);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String role = request.getHeader(ROLE_HEADER);
        if (role == null || role.isBlank()) {
            role = defaultRole;
        }
        String normalizedRole = role.toUpperCase(Locale.ROOT);
        if (!List.of("CUSTOMER", "AGENT", "MERCHANT").contains(normalizedRole)) {
            normalizedRole = "CUSTOMER";
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            "test-role-injected-user",
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }
}
