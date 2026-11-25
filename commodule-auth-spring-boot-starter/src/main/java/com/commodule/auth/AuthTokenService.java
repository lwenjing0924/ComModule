package com.commodule.auth;

import com.commodule.web.ApiException;
import io.jsonwebtoken.JwtException;

import java.util.Objects;

public class AuthTokenService {

    private final JwtTokenService jwtTokenService;
    private final RefreshTokenStore refreshTokenStore;
    private final TokenRevocationStore tokenRevocationStore;
    private final AuthUserService userService;

    public AuthTokenService(
            JwtTokenService jwtTokenService,
            RefreshTokenStore refreshTokenStore,
            TokenRevocationStore tokenRevocationStore,
            AuthUserService userService
    ) {
        this.jwtTokenService = jwtTokenService;
        this.refreshTokenStore = refreshTokenStore;
        this.tokenRevocationStore = tokenRevocationStore;
        this.userService = userService;
    }

    public LoginResponse login(AuthUser user) {
        return issue(user);
    }

    public LoginResponse refresh(String refreshToken) {
        RefreshTokenRecord record = refreshTokenStore.consume(refreshToken)
                .orElseThrow(() -> ApiException.unauthorized("刷新令牌无效或已过期"));
        AuthUser user = userService.findByUsername(record.username())
                .filter(AuthUser::enabled)
                .filter(candidate -> Objects.equals(candidate.id(), record.userId()))
                .orElseThrow(() -> ApiException.unauthorized("用户不存在或已禁用"));
        return issue(user);
    }

    public void logout(String refreshToken) {
        refreshTokenStore.revoke(refreshToken);
    }

    public void revokeAccessToken(String accessToken) {
        if (accessToken == null || accessToken.isBlank()) {
            return;
        }
        try {
            ParsedToken parsedToken = jwtTokenService.parseDetails(accessToken);
            tokenRevocationStore.revoke(parsedToken.tokenId(), parsedToken.expiresAt());
        } catch (JwtException | IllegalArgumentException ignored) {
        }
    }

    private LoginResponse issue(AuthUser user) {
        if (user.id() == null) {
            throw new IllegalStateException("登录用户缺少 id");
        }
        IssuedToken accessToken = jwtTokenService.issue(user);
        IssuedRefreshToken refreshToken = refreshTokenStore.issue(user.id(), user.username());
        return new LoginResponse(
                accessToken.token(),
                "Bearer",
                accessToken.expiresAt(),
                refreshToken.token(),
                refreshToken.expiresAt(),
                user.username(),
                user.roles()
        );
    }
}
