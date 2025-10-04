package com.wom.auth.repository.redis;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for TokenBlacklistRepository.
 */
@ExtendWith(MockitoExtension.class)
class TokenBlacklistRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private TokenBlacklistRepository tokenBlacklistRepository;

    private final String testTokenId = "test-token-123";
    private final String expectedKey = "blacklist:token:test-token-123";
    private final long expirationSeconds = 900L;

    @Test
    void blacklistToken_ShouldStoreTokenInRedis() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // When
        tokenBlacklistRepository.blacklistToken(testTokenId, expirationSeconds);

        // Then
        verify(valueOperations, times(1))
                .set(eq(expectedKey), eq("blacklisted"), eq(expirationSeconds), eq(TimeUnit.SECONDS));
    }

    @Test
    void blacklistToken_WithZeroExpiration_ShouldStoreToken() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        
        // When
        tokenBlacklistRepository.blacklistToken(testTokenId, 0L);

        // Then
        verify(valueOperations, times(1))
                .set(eq(expectedKey), eq("blacklisted"), eq(0L), eq(TimeUnit.SECONDS));
    }

    @Test
    void blacklistToken_WithLargeExpiration_ShouldStoreToken() {
        // Given
        long largeExpiration = 604800L; // 7 days
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistRepository.blacklistToken(testTokenId, largeExpiration);

        // Then
        verify(valueOperations, times(1))
                .set(eq(expectedKey), eq("blacklisted"), eq(largeExpiration), eq(TimeUnit.SECONDS));
    }

    @Test
    void isTokenBlacklisted_WithBlacklistedToken_ShouldReturnTrue() {
        // Given
        when(redisTemplate.hasKey(expectedKey)).thenReturn(true);

        // When
        boolean result = tokenBlacklistRepository.isTokenBlacklisted(testTokenId);

        // Then
        assertTrue(result);
        verify(redisTemplate, times(1)).hasKey(expectedKey);
    }

    @Test
    void isTokenBlacklisted_WithNonBlacklistedToken_ShouldReturnFalse() {
        // Given
        when(redisTemplate.hasKey(expectedKey)).thenReturn(false);

        // When
        boolean result = tokenBlacklistRepository.isTokenBlacklisted(testTokenId);

        // Then
        assertFalse(result);
        verify(redisTemplate, times(1)).hasKey(expectedKey);
    }

    @Test
    void isTokenBlacklisted_WithNullResponse_ShouldReturnFalse() {
        // Given
        when(redisTemplate.hasKey(expectedKey)).thenReturn(null);

        // When
        boolean result = tokenBlacklistRepository.isTokenBlacklisted(testTokenId);

        // Then
        assertFalse(result);
        verify(redisTemplate, times(1)).hasKey(expectedKey);
    }

    @Test
    void removeFromBlacklist_ShouldDeleteTokenFromRedis() {
        // When
        tokenBlacklistRepository.removeFromBlacklist(testTokenId);

        // Then
        verify(redisTemplate, times(1)).delete(expectedKey);
    }

    @Test
    void removeFromBlacklist_WithNonExistentToken_ShouldNotThrowException() {
        // Given
        when(redisTemplate.delete(expectedKey)).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> tokenBlacklistRepository.removeFromBlacklist(testTokenId));
        verify(redisTemplate, times(1)).delete(expectedKey);
    }

    @Test
    void blacklistToken_WithSpecialCharactersInTokenId_ShouldHandleCorrectly() {
        // Given
        String specialTokenId = "token-with-special-chars!@#$%";
        String expectedSpecialKey = "blacklist:token:token-with-special-chars!@#$%";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        // When
        tokenBlacklistRepository.blacklistToken(specialTokenId, expirationSeconds);

        // Then
        verify(valueOperations, times(1))
                .set(eq(expectedSpecialKey), eq("blacklisted"), eq(expirationSeconds), eq(TimeUnit.SECONDS));
    }

    @Test
    void isTokenBlacklisted_WithEmptyTokenId_ShouldCheckCorrectKey() {
        // Given
        String emptyTokenId = "";
        String expectedEmptyKey = "blacklist:token:";
        when(redisTemplate.hasKey(expectedEmptyKey)).thenReturn(false);

        // When
        boolean result = tokenBlacklistRepository.isTokenBlacklisted(emptyTokenId);

        // Then
        assertFalse(result);
        verify(redisTemplate, times(1)).hasKey(expectedEmptyKey);
    }

    @Test
    void removeFromBlacklist_MultipleTokens_ShouldDeleteEachSeparately() {
        // Given
        String token1 = "token1";
        String token2 = "token2";

        // When
        tokenBlacklistRepository.removeFromBlacklist(token1);
        tokenBlacklistRepository.removeFromBlacklist(token2);

        // Then
        verify(redisTemplate, times(1)).delete("blacklist:token:token1");
        verify(redisTemplate, times(1)).delete("blacklist:token:token2");
    }
}
