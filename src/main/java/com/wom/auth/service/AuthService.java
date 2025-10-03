package com.wom.auth.service;

import com.wom.auth.entity.RefreshToken;
import com.wom.auth.entity.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Service for authentication operations.
 */
@Service
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final TokenService tokenService;

    public AuthService(UserService userService, JwtService jwtService, TokenService tokenService) {
        this.userService = userService;
        this.jwtService = jwtService;
        this.tokenService = tokenService;
    }

    @Transactional
    public Map<String, Object> authenticate(String identifier, String password) {
        Optional<User> userOpt = userService.findByEmailOrUsername(identifier);
        
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        User user = userOpt.get();

        if (userService.isAccountLocked(user)) {
            throw new IllegalStateException("Account is locked");
        }

        if (!userService.isAccountActive(user)) {
            throw new IllegalStateException("Account is not active");
        }

        if (!userService.validatePassword(password, user.getPasswordHash())) {
            userService.incrementFailedAttempts(user);
            throw new IllegalArgumentException("Invalid credentials");
        }

        userService.resetFailedAttempts(user);
        userService.updateLastLogin(user);

        String accessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String refreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());
        
        tokenService.createRefreshToken(user.getId(), refreshToken);

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", accessToken);
        response.put("refreshToken", refreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 900);
        response.put("user", Map.of(
                "id", user.getId(),
                "email", user.getEmail(),
                "username", user.getUsername(),
                "fullName", user.getFullName()
        ));

        return response;
    }

    @Transactional
    public Map<String, Object> refreshAccessToken(String refreshToken) {
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
        String username = jwtService.getUsernameFromToken(refreshToken);
        
        Optional<User> userOpt = userService.findById(userId);
        if (userOpt.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }

        User user = userOpt.get();
        
        Optional<RefreshToken> newTokenOpt = tokenService.rotateRefreshToken(refreshToken);
        if (newTokenOpt.isEmpty()) {
            throw new IllegalArgumentException("Token rotation failed");
        }

        String newAccessToken = jwtService.generateAccessToken(user.getId(), user.getUsername(), user.getEmail());
        String newRefreshToken = jwtService.generateRefreshToken(user.getId(), user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("refreshToken", newRefreshToken);
        response.put("tokenType", "Bearer");
        response.put("expiresIn", 900);

        return response;
    }

    @Transactional
    public void logout(String accessToken, String refreshToken) {
        if (refreshToken != null && !refreshToken.isEmpty()) {
            tokenService.revokeRefreshToken(refreshToken);
        }

        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                if (!jwtService.isTokenExpired(accessToken)) {
                    long ttl = 900;
                    tokenService.blacklistAccessToken(accessToken, ttl);
                }
            } catch (Exception e) {
            }
        }
    }

    @Transactional
    public void logoutAllDevices(Long userId) {
        tokenService.revokeAllUserTokens(userId);
    }
}
