package com.commodule.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryRefreshTokenStore implements RefreshTokenStore {

    private static final int TOKEN_BYTES = 32;

    private final Duration tokenTtl;
    private final int maxTokensPerUser;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ConcurrentMap<String, RefreshTokenRecord> tokens = new ConcurrentHashMap<>();

    public InMemoryRefreshTokenStore(Duration tokenTtl, int maxTokensPerUser) {
        if (tokenTtl == null || tokenTtl.isZero() || tokenTtl.isNegative()) {
            throw new IllegalArgumentException("refresh token TTL 必须大于 0");
        }
        this.tokenTtl = tokenTtl;
        this.maxTokensPerUser = Math.max(1, maxTokensPerUser);
    }

    @Override
    public synchronized IssuedRefreshToken issue(Long userId, String username) {
        if (userId == null || username == null || username.isBlank()) {
            throw new IllegalArgumentException("refresh token 用户信息不能为空");
        }
        cleanupExpired();
        enforceUserLimit(userId);
        String token = generateToken();
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(tokenTtl);
        tokens.put(digest(token), new RefreshTokenRecord(userId, username, issuedAt, expiresAt));
        return new IssuedRefreshToken(token, expiresAt);
    }

    @Override
    public Optional<RefreshTokenRecord> consume(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        RefreshTokenRecord record = tokens.remove(digest(token));
        if (record == null || !record.expiresAt().isAfter(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(record);
    }

    @Override
    public void revoke(String token) {
        if (token != null && !token.isBlank()) {
            tokens.remove(digest(token));
        }
    }

    @Override
    public synchronized void revokeAll(Long userId) {
        if (userId == null) {
            return;
        }
        tokens.entrySet().removeIf(entry -> userId.equals(entry.getValue().userId()));
    }

    private void enforceUserLimit(Long userId) {
        int maxEntries = Math.max(1, maxTokensPerUser);
        List<Map.Entry<String, RefreshTokenRecord>> userTokens = tokens.entrySet().stream()
                .filter(entry -> userId.equals(entry.getValue().userId()))
                .sorted(Comparator.comparing(entry -> entry.getValue().issuedAt()))
                .toList();
        int excess = userTokens.size() - maxEntries + 1;
        for (int index = 0; index < excess; index++) {
            Map.Entry<String, RefreshTokenRecord> entry = userTokens.get(index);
            tokens.remove(entry.getKey(), entry.getValue());
        }
    }

    private void cleanupExpired() {
        Instant now = Instant.now();
        tokens.entrySet().removeIf(entry -> !entry.getValue().expiresAt().isAfter(now));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String digest(String token) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(
                    messageDigest.digest(token.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }
}
