package com.bobbuy.security;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;

public record InternalServiceIdentity(String serviceName) {
    public static final String REQUEST_ATTRIBUTE = InternalServiceIdentity.class.getName();

    public static Optional<InternalServiceIdentity> from(HttpServletRequest request) {
        Object candidate = request.getAttribute(REQUEST_ATTRIBUTE);
        if (candidate instanceof InternalServiceIdentity identity) {
            return Optional.of(identity);
        }
        return Optional.empty();
    }
}
