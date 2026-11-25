package com.commodule.auth;

import java.time.Instant;

public record RefreshTokenRecord(
        Long userId,
        String username,
        Instant issuedAt,
        Instant expiresAt
) {
}
