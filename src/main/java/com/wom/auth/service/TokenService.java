package com.wom.auth.service;

import com.wom.auth.entity.RefreshToken;
import com.wom.auth.repository.jpa.RefreshTokenRepository;
import com.wom.auth.repository.redis.TokenBlacklistRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

/**
 * Service for managing refresh tokens and token blacklist.
 */
@Service
public class TokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenBlacklistRepository tokenBlacklistRepository;
    private final JwtService jwtService;

    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;

    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    public TokenService(RefreshTokenRepository refreshTokenRepository,
                        TokenBlacklistRepository tokenBlacklistRepository,
                        JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenBlacklistRepository = tokenBlacklistRepository;
        this.jwtService = jwtService;
    }

    /**
     * Creates and persists refresh token with SHA-256 hash.
     *
     * @param userId user ID
     * @param token JWT refresh token
     * @return created RefreshToken entity
     */
    @Transactional
    public RefreshToken createRefreshToken(Long userId, String token) {
        String tokenHash = hashToken(token);

        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .tokenHash(tokenHash)
                .expiresAt(LocalDateTime.now().plusSeconds(refreshTokenExpiration / 1000))
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public Optional<RefreshToken> rotateRefreshToken(String oldToken) {
        String tokenHash = hashToken(oldToken);
        
        Optional<RefreshToken> oldTokenOpt = refreshTokenRepository.findByTokenHash(tokenHash);
        
        if (oldTokenOpt.isEmpty() || !oldTokenOpt.get().isValid()) {
            return Optional.empty();
        }

        RefreshToken oldRefreshToken = oldTokenOpt.get();
        Long userId = jwtService.getUserIdFromToken(oldToken);
        String username = jwtService.getUsernameFromToken(oldToken);
        
        String newToken = jwtService.generateRefreshToken(userId, username);
        RefreshToken newRefreshToken = createRefreshToken(userId, newToken);
        
        oldRefreshToken.revokeAndReplace(newRefreshToken.getId());
        refreshTokenRepository.save(oldRefreshToken);

        return Optional.of(newRefreshToken);
    }

    /**
     * Revokes refresh token to prevent reuse.
     *
     * @param token refresh token to revoke
     */
    @Transactional
    public void revokeRefreshToken(String token) {
        String tokenHash = hashToken(token);
        refreshTokenRepository.findByTokenHash(tokenHash)
                .ifPresent(rt -> {
                    rt.revoke();
                    refreshTokenRepository.save(rt);
                });
    }

    /**
     * Revokes all refresh tokens for a user across all devices.
     *
     * @param userId user ID
     */
    @Transactional
    public void revokeAllUserTokens(Long userId) {
        refreshTokenRepository.revokeAllUserTokens(userId, LocalDateTime.now());
    }

    /**
     * Blacklists access token in Redis with TTL.
     *
     * @param token access token
     * @param expirationSeconds TTL in seconds
     */
    public void blacklistAccessToken(String token, Long expirationSeconds) {
        tokenBlacklistRepository.blacklistToken(token, expirationSeconds);
    }

    public boolean isTokenBlacklisted(String token) {
        return tokenBlacklistRepository.isTokenBlacklisted(token);
    }

    public Optional<RefreshToken> validateRefreshToken(String token) {
        String tokenHash = hashToken(token);
        Optional<RefreshToken> refreshToken = refreshTokenRepository.findByTokenHash(tokenHash);
        
        if (refreshToken.isEmpty() || !refreshToken.get().isValid()) {
            return Optional.empty();
        }

        if (isTokenBlacklisted(token)) {
            return Optional.empty();
        }

        return refreshToken;
    }

    @Transactional
    public void cleanupExpiredTokens() {
        refreshTokenRepository.deleteExpiredTokens(LocalDateTime.now());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }
}
