package com.commodule.template.auth;

import com.commodule.auth.AuthUser;
import com.commodule.auth.AuthUserService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class DemoUserService implements AuthUserService {

    private final Map<String, AuthUser> users;

    public DemoUserService(PasswordEncoder passwordEncoder) {
        this.users = Map.of(
                "admin", new AuthUser(
                        1L,
                        "admin",
                        passwordEncoder.encode("admin123"),
                        Set.of("ADMIN", "USER"),
                        true
                ),
                "user", new AuthUser(
                        2L,
                        "user",
                        passwordEncoder.encode("user123"),
                        Set.of("USER"),
                        true
                )
        );
    }

    @Override
    public Optional<AuthUser> findByUsername(String username) {
        return Optional.ofNullable(users.get(username));
    }
}

