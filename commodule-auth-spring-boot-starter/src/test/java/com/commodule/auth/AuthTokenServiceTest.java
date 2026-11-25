package com.commodule.auth;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthTokenServiceTest {

    @Test
    void loginAndRefreshRotateTokens() {
        AuthUser user = new AuthUser(7L, "alice", "unused", Set.of("USER"), true);
        JwtTokenService jwtTokenService = jwtTokenService();
        AuthTokenService tokenService = tokenService(jwtTokenService, user);

        LoginResponse first = tokenService.login(user);
        LoginResponse second = tokenService.refresh(first.refreshToken());

        assertThat(jwtTokenService.parse(first.token()).id()).isEqualTo(7L);
        assertThat(second.token()).isNotEqualTo(first.token());
        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThatThrownBy(() -> tokenService.refresh(first.refreshToken()))
                .isInstanceOf(com.commodule.web.ApiException.class);
    }

    @Test
    void logoutRevokesRefreshToken() {
        AuthUser user = new AuthUser(7L, "alice", "unused", Set.of("USER"), true);
        JwtTokenService jwtTokenService = jwtTokenService();
        AuthTokenService tokenService = tokenService(jwtTokenService, user);
        LoginResponse login = tokenService.login(user);

        tokenService.logout(login.refreshToken());

        assertThatThrownBy(() -> tokenService.refresh(login.refreshToken()))
                .isInstanceOf(com.commodule.web.ApiException.class);
    }

    @Test
    void refreshRejectsDisabledOrChangedUser() {
        AtomicReference<AuthUser> currentUser = new AtomicReference<>(
                new AuthUser(7L, "alice", "unused", Set.of("USER"), true)
        );
        JwtTokenService jwtTokenService = jwtTokenService();
        AuthTokenService tokenService = new AuthTokenService(
                jwtTokenService,
                new InMemoryRefreshTokenStore(Duration.ofMinutes(5), 10),
                new InMemoryTokenRevocationStore(),
                username -> Optional.ofNullable(currentUser.get())
        );
        LoginResponse login = tokenService.login(currentUser.get());

        currentUser.set(new AuthUser(7L, "alice", "unused", Set.of("USER"), false));
        assertThatThrownBy(() -> tokenService.refresh(login.refreshToken()))
                .isInstanceOf(com.commodule.web.ApiException.class);

        currentUser.set(new AuthUser(7L, "alice", "unused", Set.of("USER"), true));
        LoginResponse secondLogin = tokenService.login(currentUser.get());
        currentUser.set(new AuthUser(8L, "alice", "unused", Set.of("USER"), true));
        assertThatThrownBy(() -> tokenService.refresh(secondLogin.refreshToken()))
                .isInstanceOf(com.commodule.web.ApiException.class);
    }

    private AuthTokenService tokenService(JwtTokenService jwtTokenService, AuthUser user) {
        return new AuthTokenService(
                jwtTokenService,
                new InMemoryRefreshTokenStore(Duration.ofMinutes(5), 10),
                new InMemoryTokenRevocationStore(),
                username -> Optional.of(user)
        );
    }

    private JwtTokenService jwtTokenService() {
        AuthProperties properties = new AuthProperties();
        properties.setSecret("commodule-test-secret-must-be-at-least-32-bytes");
        properties.setIssuer("test");
        properties.setTokenTtl(Duration.ofMinutes(10));
        return new JwtTokenService(properties);
    }
}
