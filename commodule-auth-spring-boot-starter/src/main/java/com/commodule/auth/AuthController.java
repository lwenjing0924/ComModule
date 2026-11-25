package com.commodule.auth;

import com.commodule.web.ApiException;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("${commodule.auth.base-path:/api/auth}")
public class AuthController {

    private final AuthService authService;
    private final AuthTokenService tokenService;

    public AuthController(AuthService authService, AuthTokenService tokenService) {
        this.authService = authService;
        this.tokenService = tokenService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthUser user = authService.authenticate(request.username(), request.password());
        return tokenService.login(user);
    }

    @PostMapping("/refresh")
    public LoginResponse refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return tokenService.refresh(request.refreshToken());
    }

    @PostMapping("/logout")
    public Map<String, Boolean> logout(
            @Valid @RequestBody RefreshTokenRequest request,
            Authentication authentication
    ) {
        tokenService.logout(request.refreshToken());
        if (authentication != null
                && authentication.getPrincipal() instanceof AuthPrincipal
                && authentication.getCredentials() instanceof String accessToken) {
            tokenService.revokeAccessToken(accessToken);
        }
        return Map.of("revoked", true);
    }

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        if (!(authentication.getPrincipal() instanceof AuthPrincipal principal)) {
            throw ApiException.unauthorized("当前认证方式不支持用户信息查询");
        }
        return new MeResponse(principal.id(), principal.username(), principal.roles());
    }
}

