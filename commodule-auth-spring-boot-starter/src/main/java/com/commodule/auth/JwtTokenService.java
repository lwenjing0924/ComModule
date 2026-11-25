package com.commodule.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class JwtTokenService {

    private final AuthProperties properties;
    private final SecretKey secretKey;

    public JwtTokenService(AuthProperties properties) {
        byte[] secret = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (secret.length < 32) {
            throw new IllegalStateException("commodule.auth.secret 至少需要 32 字节");
        }
        this.properties = properties;
        this.secretKey = Keys.hmacShaKeyFor(secret);
    }

    public IssuedToken issue(AuthUser user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.getTokenTtl());
        String token = Jwts.builder()
                .id(UUID.randomUUID().toString().replace("-", ""))
                .issuer(properties.getIssuer())
                .subject(user.username())
                .claim("uid", user.id())
                .claim("roles", List.copyOf(user.roles()))
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
        return new IssuedToken(token, expiresAt);
    }

    public AuthPrincipal parse(String token) {
        return parseDetails(token).principal();
    }

    public ParsedToken parseDetails(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(properties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        Date expiration = claims.getExpiration();
        if (expiration == null) {
            throw new JwtException("JWT 缺少过期时间");
        }
        Number userId = claims.get("uid", Number.class);
        Collection<?> rawRoles = claims.get("roles", Collection.class);
        Set<String> roles = rawRoles == null
                ? Set.of()
                : rawRoles.stream().map(String::valueOf).collect(Collectors.toUnmodifiableSet());
        AuthPrincipal principal = new AuthPrincipal(
                userId == null ? null : userId.longValue(),
                claims.getSubject(),
                roles
        );
        return new ParsedToken(principal, claims.getId(), expiration.toInstant());
    }
}

