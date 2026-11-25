package com.commodule.auth;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryTokenRevocationStore implements TokenRevocationStore {

    private final ConcurrentMap<String, Instant> revokedTokens = new ConcurrentHashMap<>();

    @Override
    public void revoke(String tokenId, Instant expiresAt) {
        if (tokenId == null || tokenId.isBlank() || expiresAt == null) {
            return;
        }
        Instant now = Instant.now();
        removeExpired(now);
        if (expiresAt.isAfter(now)) {
            revokedTokens.put(tokenId, expiresAt);
        }
    }

    @Override
    public boolean isRevoked(String tokenId) {
        if (tokenId == null || tokenId.isBlank()) {
            return false;
        }
        Instant expiresAt = revokedTokens.get(tokenId);
        if (expiresAt == null) {
            return false;
        }
        if (!expiresAt.isAfter(Instant.now())) {
            revokedTokens.remove(tokenId, expiresAt);
            return false;
        }
        return true;
    }

    private void removeExpired(Instant now) {
        revokedTokens.entrySet().removeIf(entry -> !entry.getValue().isAfter(now));
    }
}
