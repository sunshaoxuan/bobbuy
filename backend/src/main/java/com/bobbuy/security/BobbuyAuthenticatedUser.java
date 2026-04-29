package com.bobbuy.security;

import com.bobbuy.model.Role;
import java.security.Principal;

public record BobbuyAuthenticatedUser(Long id, String username, String displayName, Role role) implements Principal {
    @Override
    public String getName() {
        return id == null ? "" : String.valueOf(id);
    }
}
