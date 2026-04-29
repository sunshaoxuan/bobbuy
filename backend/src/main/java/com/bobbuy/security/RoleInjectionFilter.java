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
    public static final String USER_HEADER = "X-BOBBUY-USER";
    private static final List<String> VALID_ROLES = List.of("CUSTOMER", "AGENT", "MERCHANT");
    private final boolean enabled;
    private final String defaultRole;

    public RoleInjectionFilter(@Value("${bobbuy.security.header-auth.enabled:false}") boolean enabled,
                               @Value("${bobbuy.security.default-role:CUSTOMER}") String defaultRole) {
        this.enabled = enabled;
        this.defaultRole = defaultRole == null ? "CUSTOMER" : defaultRole.toUpperCase(Locale.ROOT);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (!enabled || SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }
        String role = request.getHeader(ROLE_HEADER);
        if (role == null || role.isBlank()) {
            role = defaultRole;
        }
        String normalizedRole = role.toUpperCase(Locale.ROOT);
        if (!VALID_ROLES.contains(normalizedRole)) {
            normalizedRole = "CUSTOMER";
        }
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
            resolvePrincipal(request, normalizedRole),
            null,
            List.of(new SimpleGrantedAuthority("ROLE_" + normalizedRole))
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !enabled;
    }

    private String resolvePrincipal(HttpServletRequest request, String role) {
        String user = request.getHeader(USER_HEADER);
        if (user == null || user.isBlank()) {
            return "role-injected-" + role.toLowerCase(Locale.ROOT);
        }
        return user.trim();
    }
}
