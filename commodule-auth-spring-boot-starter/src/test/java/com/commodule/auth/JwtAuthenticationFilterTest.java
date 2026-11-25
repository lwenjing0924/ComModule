package com.commodule.auth;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.Duration;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthenticationFilterTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void keepsExistingAuthentication() throws Exception {
        JwtTokenService tokenService = new JwtTokenService(properties());
        String token = tokenService.issue(
                new AuthUser(9L, "alice", "unused", Set.of("USER"), true)
        ).token();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("existing", null, List.of())
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        new JwtAuthenticationFilter(tokenService, new InMemoryTokenRevocationStore()).doFilter(
                request,
                new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> {
                }
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication().getPrincipal())
                .isEqualTo("existing");
    }

    @Test
    void rejectsRevokedToken() throws Exception {
        JwtTokenService tokenService = new JwtTokenService(properties());
        String token = tokenService.issue(
                new AuthUser(9L, "alice", "unused", Set.of("USER"), true)
        ).token();
        ParsedToken parsedToken = tokenService.parseDetails(token);
        InMemoryTokenRevocationStore revocationStore = new InMemoryTokenRevocationStore();
        revocationStore.revoke(parsedToken.tokenId(), parsedToken.expiresAt());

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/demo/me");
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        new JwtAuthenticationFilter(tokenService, revocationStore).doFilter(
                request,
                new MockHttpServletResponse(),
                (servletRequest, servletResponse) -> {
                }
        );

        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    private AuthProperties properties() {
        AuthProperties properties = new AuthProperties();
        properties.setSecret("commodule-test-secret-must-be-at-least-32-bytes");
        properties.setIssuer("test");
        properties.setTokenTtl(Duration.ofMinutes(10));
        return properties;
    }
}
