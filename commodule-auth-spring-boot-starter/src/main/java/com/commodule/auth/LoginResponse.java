package com.commodule.auth;

import java.time.Instant;
import java.util.Set;

public record LoginResponse(
        String token,
        String tokenType,
        Instant expiresAt,
        String refreshToken,
        Instant refreshExpiresAt,
        String username,
        Set<String> roles
) {
}

