package com.commodule.auth;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String BEARER_SCHEME = "Bearer";

    private final JwtTokenService tokenService;
    private final TokenRevocationStore revocationStore;

    public JwtAuthenticationFilter(JwtTokenService tokenService, TokenRevocationStore revocationStore) {
        this.tokenService = tokenService;
        this.revocationStore = revocationStore;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
            String token = bearerToken(authorization);
            if (token != null) {
                authenticate(request, token);
            }
        }
        filterChain.doFilter(request, response);
    }

    private String bearerToken(String authorization) {
        if (authorization == null) {
            return null;
        }
        String value = authorization.trim();
        int separator = value.indexOf(' ');
        if (separator <= 0 || !BEARER_SCHEME.equalsIgnoreCase(value.substring(0, separator))) {
            return null;
        }
        String token = value.substring(separator + 1).trim();
        return token.isEmpty() ? null : token;
    }

    private void authenticate(HttpServletRequest request, String token) {
        try {
            ParsedToken parsedToken = tokenService.parseDetails(token);
            if (revocationStore.isRevoked(parsedToken.tokenId())) {
                SecurityContextHolder.clearContext();
                return;
            }
            AuthPrincipal principal = parsedToken.principal();
            List<SimpleGrantedAuthority> authorities = principal.roles().stream()
                    .map(this::toAuthority)
                    .toList();
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(principal, token, authorities);
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (JwtException | IllegalArgumentException exception) {
            SecurityContextHolder.clearContext();
        }
    }

    private SimpleGrantedAuthority toAuthority(String role) {
        return new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role);
    }
}

