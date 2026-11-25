package com.commodule.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "commodule.auth")
public class AuthProperties {

    private String secret = "commodule-change-me-32-byte-secret-key";
    private Duration tokenTtl = Duration.ofHours(2);
    private String issuer = "commodule";
    private String basePath = "/api/auth";
    private Duration refreshTokenTtl = Duration.ofDays(7);
    private int refreshTokenMaxPerUser = 10;
    private List<String> corsAllowedOrigins = List.of();
    private List<String> corsAllowedMethods = List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS");
    private List<String> corsAllowedHeaders = List.of("*");
    private List<String> corsExposedHeaders = List.of("X-Trace-Id");
    private boolean corsAllowCredentials = false;
    private Duration corsMaxAge = Duration.ofHours(1);

    public String getSecret() {
        return secret;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public Duration getTokenTtl() {
        return tokenTtl;
    }

    public void setTokenTtl(Duration tokenTtl) {
        this.tokenTtl = tokenTtl;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public Duration getRefreshTokenTtl() {
        return refreshTokenTtl;
    }

    public void setRefreshTokenTtl(Duration refreshTokenTtl) {
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public int getRefreshTokenMaxPerUser() {
        return refreshTokenMaxPerUser;
    }

    public void setRefreshTokenMaxPerUser(int refreshTokenMaxPerUser) {
        this.refreshTokenMaxPerUser = refreshTokenMaxPerUser;
    }

    public List<String> getCorsAllowedOrigins() {
        return corsAllowedOrigins;
    }

    public void setCorsAllowedOrigins(List<String> corsAllowedOrigins) {
        this.corsAllowedOrigins = corsAllowedOrigins == null ? List.of() : List.copyOf(corsAllowedOrigins);
    }

    public List<String> getCorsAllowedMethods() {
        return corsAllowedMethods;
    }

    public void setCorsAllowedMethods(List<String> corsAllowedMethods) {
        this.corsAllowedMethods = corsAllowedMethods == null ? List.of() : List.copyOf(corsAllowedMethods);
    }

    public List<String> getCorsAllowedHeaders() {
        return corsAllowedHeaders;
    }

    public void setCorsAllowedHeaders(List<String> corsAllowedHeaders) {
        this.corsAllowedHeaders = corsAllowedHeaders == null ? List.of() : List.copyOf(corsAllowedHeaders);
    }

    public List<String> getCorsExposedHeaders() {
        return corsExposedHeaders;
    }

    public void setCorsExposedHeaders(List<String> corsExposedHeaders) {
        this.corsExposedHeaders = corsExposedHeaders == null ? List.of() : List.copyOf(corsExposedHeaders);
    }

    public boolean isCorsAllowCredentials() {
        return corsAllowCredentials;
    }

    public void setCorsAllowCredentials(boolean corsAllowCredentials) {
        this.corsAllowCredentials = corsAllowCredentials;
    }

    public Duration getCorsMaxAge() {
        return corsMaxAge;
    }

    public void setCorsMaxAge(Duration corsMaxAge) {
        this.corsMaxAge = corsMaxAge;
    }

    public String normalizedBasePath() {
        String normalized = basePath == null || basePath.isBlank() ? "/api/auth" : basePath.trim();
        if (!normalized.startsWith("/")) {
            normalized = "/" + normalized;
        }
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    public String loginPath() {
        return normalizedBasePath() + "/login";
    }

    public String refreshPath() {
        return normalizedBasePath() + "/refresh";
    }

    public String logoutPath() {
        return normalizedBasePath() + "/logout";
    }
}

