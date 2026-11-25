package com.commodule.auth;

import java.util.Optional;

public interface AuthUserService {

    Optional<AuthUser> findByUsername(String username);
}

