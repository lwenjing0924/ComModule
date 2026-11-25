package com.commodule.auth;

import com.commodule.web.ApiException;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthServiceTest {

    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @Test
    void authenticatesValidCredentials() {
        AuthService authService = new AuthService(
                username -> Optional.of(new AuthUser(
                        1L,
                        "alice",
                        passwordEncoder.encode("secret"),
                        Set.of("USER"),
                        true
                )),
                passwordEncoder
        );

        AuthUser user = authService.authenticate("alice", "secret");

        assertThat(user.username()).isEqualTo("alice");
    }

    @Test
    void rejectsWrongPassword() {
        AuthService authService = new AuthService(
                username -> Optional.of(new AuthUser(
                        1L,
                        "alice",
                        passwordEncoder.encode("secret"),
                        Set.of("USER"),
                        true
                )),
                passwordEncoder
        );

        assertThatThrownBy(() -> authService.authenticate("alice", "wrong"))
                .isInstanceOf(ApiException.class);
    }
}

