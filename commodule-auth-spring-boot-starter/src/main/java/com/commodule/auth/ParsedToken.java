package com.commodule.auth;

import java.time.Instant;

public record ParsedToken(
        AuthPrincipal principal,
        String tokenId,
        Instant expiresAt
) {
}
