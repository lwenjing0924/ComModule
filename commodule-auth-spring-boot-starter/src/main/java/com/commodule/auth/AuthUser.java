package com.commodule.auth;

import java.util.Set;

public record AuthUser(
        Long id,
        String username,
        String passwordHash,
        Set<String> roles,
        boolean enabled
) {

    public AuthUser {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }
}

