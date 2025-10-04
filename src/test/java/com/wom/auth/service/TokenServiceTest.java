package com.wom.auth.service;

import com.wom.auth.entity.RefreshToken;
import com.wom.auth.repository.jpa.RefreshTokenRepository;
import com.wom.auth.repository.redis.TokenBlacklistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link TokenService}.
 */
@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private TokenBlacklistRepository tokenBlacklistRepository;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private TokenService tokenService;

    private RefreshToken testRefreshToken;
    private final String testToken = "test.refresh.token";
    private final Long testUserId = 1L;
    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(tokenService, "refreshTokenExpiration", 604800000L);
        ReflectionTestUtils.setField(tokenService, "accessTokenExpiration", 900000L);
        
        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .userId(testUserId)
                .tokenHash("hashedToken")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void createRefreshToken_WithValidData_ShouldReturnSavedToken() {
        // Arrange
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        RefreshToken result = tokenService.createRefreshToken(testUserId, testToken);

        // Assert
        assertNotNull(result);
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_WithValidToken_ShouldReturnNewToken() {
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(testRefreshToken));
        when(jwtService.getUserIdFromToken(testToken)).thenReturn(testUserId);
        when(jwtService.getUsernameFromToken(testToken)).thenReturn(testUsername);
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn("new.refresh.token");
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenReturn(testRefreshToken);

        // Act
        Optional<RefreshToken> result = tokenService.rotateRefreshToken(testToken);

        // Assert
        assertTrue(result.isPresent());
        verify(refreshTokenRepository, times(2)).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_WithInvalidToken_ShouldReturnEmpty() {
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act
        Optional<RefreshToken> result = tokenService.rotateRefreshToken(testToken);

        // Assert
        assertFalse(result.isPresent());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void rotateRefreshToken_WithRevokedToken_ShouldReturnEmpty() {
        // Arrange
        testRefreshToken.revoke();
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(testRefreshToken));

        // Act
        Optional<RefreshToken> result = tokenService.rotateRefreshToken(testToken);

        // Assert
        assertFalse(result.isPresent());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void revokeRefreshToken_WithExistingToken_ShouldRevokeAndSave() {
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(testRefreshToken));

        // Act
        tokenService.revokeRefreshToken(testToken);

        // Assert
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    void revokeRefreshToken_WithNonExistingToken_ShouldNotSave() {
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act
        tokenService.revokeRefreshToken(testToken);

        // Assert
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void revokeAllUserTokens_ShouldCallRepositoryMethod() {
        // Arrange
        Long userId = 1L;

        // Act
        tokenService.revokeAllUserTokens(userId);

        // Assert
        verify(refreshTokenRepository, times(1))
                .revokeAllUserTokens(eq(userId), any(LocalDateTime.class));
    }

    @Test
    void blacklistAccessToken_ShouldCallRepositoryMethod() {
        // Arrange
        String token = "access.token";
        Long expirationSeconds = 900L;

        // Act
        tokenService.blacklistAccessToken(token, expirationSeconds);

        // Assert
        verify(tokenBlacklistRepository, times(1))
                .blacklistToken(token, expirationSeconds);
    }

    @Test
    void isTokenBlacklisted_WithBlacklistedToken_ShouldReturnTrue() {
        // Arrange
        when(tokenBlacklistRepository.isTokenBlacklisted(testToken)).thenReturn(true);

        // Act
        boolean result = tokenService.isTokenBlacklisted(testToken);

        // Assert
        assertTrue(result);
        verify(tokenBlacklistRepository, times(1)).isTokenBlacklisted(testToken);
    }

    @Test
    void isTokenBlacklisted_WithNonBlacklistedToken_ShouldReturnFalse() {
        // Arrange
        when(tokenBlacklistRepository.isTokenBlacklisted(testToken)).thenReturn(false);

        // Act
        boolean result = tokenService.isTokenBlacklisted(testToken);

        // Assert
        assertFalse(result);
        verify(tokenBlacklistRepository, times(1)).isTokenBlacklisted(testToken);
    }

    @Test
    void validateRefreshToken_WithValidToken_ShouldReturnToken() {
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(testRefreshToken));
        when(tokenBlacklistRepository.isTokenBlacklisted(anyString())).thenReturn(false);

        // Act
        Optional<RefreshToken> result = tokenService.validateRefreshToken(testToken);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(testRefreshToken, result.get());
    }

    @Test
    void validateRefreshToken_WithNonExistingToken_ShouldReturnEmpty() {
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

        // Act
        Optional<RefreshToken> result = tokenService.validateRefreshToken(testToken);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void validateRefreshToken_WithBlacklistedToken_ShouldReturnEmpty() {
        // Arrange
        when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(testRefreshToken));
        when(tokenBlacklistRepository.isTokenBlacklisted(anyString())).thenReturn(true);

        // Act
        Optional<RefreshToken> result = tokenService.validateRefreshToken(testToken);

        // Assert
        assertFalse(result.isPresent());
    }

    @Test
    void cleanupExpiredTokens_ShouldCallRepositoryMethod() {
        // Act
        tokenService.cleanupExpiredTokens();

        // Assert
        verify(refreshTokenRepository, times(1))
                .deleteExpiredTokens(any(LocalDateTime.class));
    }
}
