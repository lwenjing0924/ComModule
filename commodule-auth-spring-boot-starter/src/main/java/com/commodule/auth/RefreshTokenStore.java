package com.commodule.auth;

import java.util.Optional;

public interface RefreshTokenStore {

    IssuedRefreshToken issue(Long userId, String username);

    Optional<RefreshTokenRecord> consume(String token);

    void revoke(String token);

    void revokeAll(Long userId);
}
