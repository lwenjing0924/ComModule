package com.commodule.auth;

import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    @Test
    void issuesAndParsesToken() {
        AuthProperties properties = properties();
        JwtTokenService tokenService = new JwtTokenService(properties);
        AuthUser user = new AuthUser(7L, "alice", "unused", Set.of("ADMIN", "USER"), true);

        IssuedToken issuedToken = tokenService.issue(user);
        AuthPrincipal principal = tokenService.parse(issuedToken.token());
        ParsedToken parsedToken = tokenService.parseDetails(issuedToken.token());

        assertThat(issuedToken.expiresAt()).isAfter(java.time.Instant.now());
        assertThat(principal.id()).isEqualTo(7L);
        assertThat(principal.username()).isEqualTo("alice");
        assertThat(principal.roles()).containsExactlyInAnyOrder("ADMIN", "USER");
        assertThat(parsedToken.tokenId()).isNotBlank();
        assertThat(parsedToken.expiresAt()).isAfter(java.time.Instant.now());
    }

    @Test
    void rejectsTamperedToken() {
        JwtTokenService tokenService = new JwtTokenService(properties());
        AuthUser user = new AuthUser(1L, "alice", "unused", Set.of("USER"), true);
        String token = tokenService.issue(user).token();

        assertThatThrownBy(() -> tokenService.parse(token + "x"))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void rejectsShortSecret() {
        AuthProperties properties = properties();
        properties.setSecret("short");

        assertThatThrownBy(() -> new JwtTokenService(properties))
                .isInstanceOf(IllegalStateException.class);
    }

    private AuthProperties properties() {
        AuthProperties properties = new AuthProperties();
        properties.setSecret("commodule-test-secret-must-be-at-least-32-bytes");
        properties.setIssuer("test");
        properties.setTokenTtl(Duration.ofMinutes(10));
        return properties;
    }
}

