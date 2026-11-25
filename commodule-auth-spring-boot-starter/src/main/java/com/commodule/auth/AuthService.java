package com.commodule.auth;

import com.commodule.web.ApiException;
import org.springframework.security.crypto.password.PasswordEncoder;

public class AuthService {

    private final AuthUserService userService;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthUserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthUser authenticate(String username, String password) {
        AuthUser user = userService.findByUsername(username)
                .orElseThrow(() -> ApiException.unauthorized("用户名或密码错误"));
        if (!user.enabled() || !passwordEncoder.matches(password, user.passwordHash())) {
            throw ApiException.unauthorized("用户名或密码错误");
        }
        return user;
    }
}

