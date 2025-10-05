package com.wom.auth.service;

import com.wom.auth.dto.LoginResponse;
import com.wom.auth.entity.RefreshToken;
import com.wom.auth.entity.User;
import com.wom.auth.exception.AccountLockedException;
import com.wom.auth.exception.InvalidCredentialsException;
import com.wom.auth.metrics.MetricsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Service for authentication operations.
 * Handles login, token refresh, and logout with metrics recording.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final MetricsService metricsService;
    private final AuditService auditService;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    public AuthService(UserService userService, JwtService jwtService, TokenService tokenService, 
                      MetricsService metricsService, AuditService auditService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.metricsService = metricsService;
        this.auditService = auditService;
    }

    /**
     * Authenticates a user with credentials.
     * Manages failed attempts and account locking (5 attempts = 30 min lock).
     *
     * @param identifier user's email or username
     * @param password plain text password
     * @param request HTTP request for audit logging
     * @return LoginResponse with access and refresh tokens
     * @throws InvalidCredentialsException if credentials invalid or account inactive
     * @throws AccountLockedException if account locked due to failed attempts
     */
    @Transactional
    public LoginResponse authenticate(String identifier, String password, HttpServletRequest request) {
        return metricsService.recordLoginOperation(() -> {
            Optional<User> userOpt = userService.findByEmailOrUsername(identifier);
            
            if (userOpt.isEmpty()) {
                auditService.logLoginAttempt(null, identifier, false, "User not found", request);
                throw new InvalidCredentialsException("Invalid credentials");
            }

            User user = userOpt.get();

            if (userService.isAccountLocked(user)) {
                auditService.logAccountLocked(user.getId(), identifier, 
                    "Account locked after multiple failed attempts", request);
                throw new AccountLockedException("Account is locked", user.getLockedUntil());
            }

            if (!userService.isAccountActive(user)) {
                auditService.logLoginAttempt(user.getId(), identifier, false, "Account not active", request);
                throw new InvalidCredentialsException("Account is not active");
            }

            if (!userService.validatePassword(password, user.getPasswordHash())) {
                userService.incrementFailedAttempts(user);
                auditService.logLoginAttempt(user.getId(), identifier, false, "Invalid password", request);
                throw new InvalidCredentialsException("Invalid credentials");
            }

            userService.resetFailedAttempts(user);
            userService.updateLastLogin(user);

            String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());
            
            tokenService.createRefreshToken(user.getId(), refreshToken);

            // Log successful login
            auditService.logLoginAttempt(user.getId(), identifier, true, null, request);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenExpiration / 1000)
                    .build();
        });
    }

    /**
     * Refreshes access token using valid refresh token.
     * Implements token rotation by invalidating old refresh token.
     *
     * @param refreshToken current valid refresh token
     * @param request HTTP request for audit logging
     * @return LoginResponse with new access and refresh tokens
     * @throws IllegalArgumentException if token invalid, expired, or user not found
     */
    @Transactional
    public LoginResponse refreshAccessToken(String refreshToken, HttpServletRequest request) {
        return metricsService.recordRefreshOperation(() -> {
            Optional<RefreshToken> validToken = tokenService.validateRefreshToken(refreshToken);
            
            if (validToken.isEmpty()) {
                auditService.logRefreshToken(null, false, "Invalid or expired refresh token", request);
                throw new IllegalArgumentException("Invalid or expired refresh token");
            }

            try {
                jwtService.validateToken(refreshToken);
            } catch (Exception e) {
                auditService.logRefreshToken(null, false, "Token validation failed: " + e.getMessage(), request);
                throw new IllegalArgumentException("Invalid refresh token", e);
            }

            Long userId = jwtService.getUserIdFromToken(refreshToken);
            
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                auditService.logRefreshToken(userId, false, "User not found", request);
                throw new IllegalArgumentException("User not found");
            }

            User user = userOpt.get();

            String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
            String newRefreshTokenJwt = jwtService.generateRefreshToken(user.getId(), user.getUsername());
            
            tokenService.revokeRefreshToken(refreshToken);
            tokenService.createRefreshToken(user.getId(), newRefreshTokenJwt);

            // Log successful token refresh
            auditService.logRefreshToken(user.getId(), true, null, request);

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshTokenJwt)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenExpiration / 1000)
                    .build();
        });
    }

    /**
     * Logs out user by blacklisting access token and revoking refresh token.
     *
     * @param accessToken current access token to invalidate
     * @param request HTTP request for audit logging
     */
    @Transactional
    public void logout(String accessToken, HttpServletRequest request) {
        metricsService.recordLogout();
        
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                Long userId = jwtService.getUserIdFromToken(accessToken);
                
                if (!jwtService.isTokenExpired(accessToken)) {
                    long ttl = accessTokenExpiration / 1000;
                    tokenService.blacklistAccessToken(accessToken, ttl);
                }
                
                String username = jwtService.getUsernameFromToken(accessToken);
                String refreshToken = jwtService.generateRefreshToken(userId, username);
                tokenService.revokeRefreshToken(refreshToken);
                
                // Log successful logout
                auditService.logLogout(userId, false, request);
            } catch (Exception e) {
                // Log failed logout attempt if applicable
            }
        }
    }

    /**
     * Logs out user from all devices by revoking all refresh tokens.
     *
     * @param accessToken current access token to identify user
     * @param request HTTP request for audit logging
     */
    @Transactional
    public void logoutAllDevices(String accessToken, HttpServletRequest request) {
        metricsService.recordLogout();
        
        try {
            Long userId = jwtService.getUserIdFromToken(accessToken);
            tokenService.revokeAllUserTokens(userId);
            
            if (!jwtService.isTokenExpired(accessToken)) {
                long ttl = accessTokenExpiration / 1000;
                tokenService.blacklistAccessToken(accessToken, ttl);
            }
            
            // Log successful logout from all devices
            auditService.logLogout(userId, true, request);
        } catch (Exception e) {
            // Log failed logout attempt if applicable
        }
    }
}
