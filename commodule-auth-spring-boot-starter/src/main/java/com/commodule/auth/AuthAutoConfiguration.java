package com.commodule.auth;

import com.commodule.web.ApiResponse;
import com.commodule.web.ErrorCode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@AutoConfiguration(before = UserDetailsServiceAutoConfiguration.class)
@EnableMethodSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@EnableConfigurationProperties(AuthProperties.class)
public class AuthAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public AuthenticationManager authenticationManager() {
        return authentication -> {
            throw new AuthenticationServiceException("请通过 /api/auth/login 获取 JWT");
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtTokenService jwtTokenService(AuthProperties properties) {
        return new JwtTokenService(properties);
    }

    @Bean
    @ConditionalOnMissingBean
    public TokenRevocationStore tokenRevocationStore() {
        return new InMemoryTokenRevocationStore();
    }

    @Bean
    @ConditionalOnMissingBean
    public JwtAuthenticationFilter jwtAuthenticationFilter(
            JwtTokenService tokenService,
            TokenRevocationStore revocationStore
    ) {
        return new JwtAuthenticationFilter(tokenService, revocationStore);
    }

    @Bean
    @ConditionalOnMissingBean
    public RefreshTokenStore refreshTokenStore(AuthProperties properties) {
        return new InMemoryRefreshTokenStore(
                properties.getRefreshTokenTtl(),
                properties.getRefreshTokenMaxPerUser()
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AuthUserService.class)
    public AuthService authService(AuthUserService userService, PasswordEncoder passwordEncoder) {
        return new AuthService(userService, passwordEncoder);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean(AuthUserService.class)
    public AuthTokenService authTokenService(
            JwtTokenService jwtTokenService,
            RefreshTokenStore refreshTokenStore,
            TokenRevocationStore tokenRevocationStore,
            AuthUserService userService
    ) {
        return new AuthTokenService(
                jwtTokenService,
                refreshTokenStore,
                tokenRevocationStore,
                userService
        );
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnBean({AuthService.class, AuthTokenService.class})
    public AuthController authController(AuthService authService, AuthTokenService tokenService) {
        return new AuthController(authService, tokenService);
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthExceptionHandler authExceptionHandler() {
        return new AuthExceptionHandler();
    }

    @Bean
    @ConditionalOnMissingBean(name = "corsConfigurationSource")
    public UrlBasedCorsConfigurationSource corsConfigurationSource(AuthProperties properties) {
        CorsConfiguration configuration = new CorsConfiguration();
        if (!properties.getCorsAllowedOrigins().isEmpty()) {
            configuration.setAllowedOriginPatterns(properties.getCorsAllowedOrigins());
        }
        configuration.setAllowedMethods(properties.getCorsAllowedMethods());
        configuration.setAllowedHeaders(properties.getCorsAllowedHeaders());
        configuration.setExposedHeaders(properties.getCorsExposedHeaders());
        configuration.setAllowCredentials(properties.isCorsAllowCredentials());
        configuration.setMaxAge(properties.getCorsMaxAge().toSeconds());
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain.class)
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthProperties properties,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            ObjectMapper objectMapper,
            @Qualifier("corsConfigurationSource") CorsConfigurationSource corsConfigurationSource
    ) throws Exception {
        CorsFilter corsFilter = new CorsFilter(corsConfigurationSource);
        corsFilter.setCorsProcessor(new ApiCorsProcessor(objectMapper));
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(
                                HttpMethod.POST,
                                properties.loginPath(),
                                properties.refreshPath(),
                                properties.logoutPath()
                        ).permitAll()
                        .requestMatchers("/actuator/health", "/error").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exceptionHandling -> exceptionHandling
                        .authenticationEntryPoint((request, response, exception) ->
                                writeError(objectMapper, response, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED))
                        .accessDeniedHandler((request, response, exception) ->
                                writeError(objectMapper, response, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN))
                )
                .addFilterBefore(corsFilter, SecurityContextHolderFilter.class)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private void writeError(
            ObjectMapper objectMapper,
            HttpServletResponse response,
            HttpStatus status,
            ErrorCode errorCode
    ) throws IOException {
        response.setStatus(status.value());
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(errorCode));
    }
}

