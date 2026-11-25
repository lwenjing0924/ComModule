package com.commodule.auth;

import java.security.Principal;
import java.util.Set;

public record AuthPrincipal(Long id, String username, Set<String> roles) implements Principal {

    public AuthPrincipal {
        roles = roles == null ? Set.of() : Set.copyOf(roles);
    }

    @Override
    public String getName() {
        return username;
    }
}

