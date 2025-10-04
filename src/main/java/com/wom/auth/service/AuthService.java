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

import java.util.Optional;

/**
 * Service for authentication operations.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final TokenService tokenService;
    private final MetricsService metricsService;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    public AuthService(UserService userService, JwtService jwtService, TokenService tokenService, MetricsService metricsService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
        this.metricsService = metricsService;
    }

    @Transactional
    public LoginResponse authenticate(String identifier, String password) {
        return metricsService.recordLoginOperation(() -> {
            Optional<User> userOpt = userService.findByEmailOrUsername(identifier);
            
            if (userOpt.isEmpty()) {
                throw new InvalidCredentialsException("Invalid credentials");
            }

            User user = userOpt.get();

            if (userService.isAccountLocked(user)) {
                throw new AccountLockedException("Account is locked", user.getLockedUntil());
            }

            if (!userService.isAccountActive(user)) {
                throw new InvalidCredentialsException("Account is not active");
            }

            if (!userService.validatePassword(password, user.getPasswordHash())) {
                userService.incrementFailedAttempts(user);
                throw new InvalidCredentialsException("Invalid credentials");
            }

            userService.resetFailedAttempts(user);
            userService.updateLastLogin(user);

            String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
            String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());
            
            tokenService.createRefreshToken(user.getId(), refreshToken);

            return LoginResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenExpiration / 1000)
                    .build();
        });
    }

    @Transactional
    public LoginResponse refreshAccessToken(String refreshToken) {
        return metricsService.recordRefreshOperation(() -> {
            Optional<RefreshToken> validToken = tokenService.validateRefreshToken(refreshToken);
            
            if (validToken.isEmpty()) {
                throw new IllegalArgumentException("Invalid or expired refresh token");
            }

            try {
                jwtService.validateToken(refreshToken);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid refresh token", e);
            }

            Long userId = jwtService.getUserIdFromToken(refreshToken);
            
            Optional<User> userOpt = userService.findById(userId);
            if (userOpt.isEmpty()) {
                throw new IllegalArgumentException("User not found");
            }

            User user = userOpt.get();

            String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
            String newRefreshTokenJwt = jwtService.generateRefreshToken(user.getId(), user.getUsername());
            
            tokenService.revokeRefreshToken(refreshToken);
            tokenService.createRefreshToken(user.getId(), newRefreshTokenJwt);

            return LoginResponse.builder()
                    .accessToken(newAccessToken)
                    .refreshToken(newRefreshTokenJwt)
                    .tokenType("Bearer")
                    .expiresIn(accessTokenExpiration / 1000)
                    .build();
        });
    }

    @Transactional
    public void logout(String accessToken) {
        metricsService.recordLogout();
        
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                if (!jwtService.isTokenExpired(accessToken)) {
                    long ttl = accessTokenExpiration / 1000;
                    tokenService.blacklistAccessToken(accessToken, ttl);
                }
                
                Long userId = jwtService.getUserIdFromToken(accessToken);
                String username = jwtService.getUsernameFromToken(accessToken);
                String refreshToken = jwtService.generateRefreshToken(userId, username);
                tokenService.revokeRefreshToken(refreshToken);
            } catch (Exception e) {
            }
        }
    }

    @Transactional
    public void logoutAllDevices(String accessToken) {
        metricsService.recordLogout();
        
        try {
            Long userId = jwtService.getUserIdFromToken(accessToken);
            tokenService.revokeAllUserTokens(userId);
            
            if (!jwtService.isTokenExpired(accessToken)) {
                long ttl = accessTokenExpiration / 1000;
                tokenService.blacklistAccessToken(accessToken, ttl);
            }
        } catch (Exception e) {
        }
    }
}
