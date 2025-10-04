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
 * Unit tests for RateLimitRepository.
 */
@ExtendWith(MockitoExtension.class)
class RateLimitRepositoryTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitRepository rateLimitRepository;

    private final String testKey = "user:123";
    private final String expectedRedisKey = "rate_limit:user:123";
    private final long duration = 60L;
    private final TimeUnit timeUnit = TimeUnit.SECONDS;

    @Test
    void increment_FirstAttempt_ShouldReturnOneAndSetExpiration() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(expectedRedisKey)).thenReturn(1L);

        // When
        Long result = rateLimitRepository.increment(testKey, duration, timeUnit);

        // Then
        assertEquals(1L, result);
        verify(valueOperations, times(1)).increment(expectedRedisKey);
        verify(redisTemplate, times(1)).expire(expectedRedisKey, duration, timeUnit);
    }

    @Test
    void increment_SecondAttempt_ShouldReturnTwoAndNotSetExpiration() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(expectedRedisKey)).thenReturn(2L);

        // When
        Long result = rateLimitRepository.increment(testKey, duration, timeUnit);

        // Then
        assertEquals(2L, result);
        verify(valueOperations, times(1)).increment(expectedRedisKey);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void increment_MultipleAttempts_ShouldIncrementCorrectly() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(expectedRedisKey))
                .thenReturn(1L, 2L, 3L, 4L, 5L);

        // When
        Long attempt1 = rateLimitRepository.increment(testKey, duration, timeUnit);
        Long attempt2 = rateLimitRepository.increment(testKey, duration, timeUnit);
        Long attempt3 = rateLimitRepository.increment(testKey, duration, timeUnit);
        Long attempt4 = rateLimitRepository.increment(testKey, duration, timeUnit);
        Long attempt5 = rateLimitRepository.increment(testKey, duration, timeUnit);

        // Then
        assertEquals(1L, attempt1);
        assertEquals(2L, attempt2);
        assertEquals(3L, attempt3);
        assertEquals(4L, attempt4);
        assertEquals(5L, attempt5);
        verify(redisTemplate, times(1)).expire(expectedRedisKey, duration, timeUnit);
    }

    @Test
    void increment_WithNullCount_ShouldHandleGracefully() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(expectedRedisKey)).thenReturn(null);

        // When
        Long result = rateLimitRepository.increment(testKey, duration, timeUnit);

        // Then
        assertNull(result);
        verify(redisTemplate, never()).expire(anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    void getAttempts_WithExistingKey_ShouldReturnCount() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedRedisKey)).thenReturn("5");

        // When
        Long result = rateLimitRepository.getAttempts(testKey);

        // Then
        assertEquals(5L, result);
        verify(valueOperations, times(1)).get(expectedRedisKey);
    }

    @Test
    void getAttempts_WithNonExistingKey_ShouldReturnZero() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedRedisKey)).thenReturn(null);

        // When
        Long result = rateLimitRepository.getAttempts(testKey);

        // Then
        assertEquals(0L, result);
        verify(valueOperations, times(1)).get(expectedRedisKey);
    }

    @Test
    void getAttempts_WithZeroAttempts_ShouldReturnZero() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedRedisKey)).thenReturn("0");

        // When
        Long result = rateLimitRepository.getAttempts(testKey);

        // Then
        assertEquals(0L, result);
        verify(valueOperations, times(1)).get(expectedRedisKey);
    }

    @Test
    void reset_ShouldDeleteKey() {
        // When
        rateLimitRepository.reset(testKey);

        // Then
        verify(redisTemplate, times(1)).delete(expectedRedisKey);
    }

    @Test
    void reset_WithNonExistingKey_ShouldNotThrowException() {
        // Given
        when(redisTemplate.delete(expectedRedisKey)).thenReturn(false);

        // When & Then
        assertDoesNotThrow(() -> rateLimitRepository.reset(testKey));
        verify(redisTemplate, times(1)).delete(expectedRedisKey);
    }

    @Test
    void getTimeToLive_WithExistingKey_ShouldReturnTTL() {
        // Given
        Long expectedTTL = 45L;
        when(redisTemplate.getExpire(expectedRedisKey, TimeUnit.SECONDS)).thenReturn(expectedTTL);

        // When
        Long result = rateLimitRepository.getTimeToLive(testKey);

        // Then
        assertEquals(expectedTTL, result);
        verify(redisTemplate, times(1)).getExpire(expectedRedisKey, TimeUnit.SECONDS);
    }

    @Test
    void getTimeToLive_WithExpiredKey_ShouldReturnNegative() {
        // Given
        when(redisTemplate.getExpire(expectedRedisKey, TimeUnit.SECONDS)).thenReturn(-2L);

        // When
        Long result = rateLimitRepository.getTimeToLive(testKey);

        // Then
        assertEquals(-2L, result);
        verify(redisTemplate, times(1)).getExpire(expectedRedisKey, TimeUnit.SECONDS);
    }

    @Test
    void increment_WithDifferentTimeUnits_ShouldHandleCorrectly() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(expectedRedisKey)).thenReturn(1L);

        // When - Minutes
        rateLimitRepository.increment(testKey, 5L, TimeUnit.MINUTES);

        // Then
        verify(redisTemplate, times(1)).expire(expectedRedisKey, 5L, TimeUnit.MINUTES);
    }

    @Test
    void increment_WithHoursTimeUnit_ShouldSetExpirationCorrectly() {
        // Given
        String hourlyKey = "hourly:limit";
        String expectedHourlyKey = "rate_limit:hourly:limit";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(expectedHourlyKey)).thenReturn(1L);

        // When
        rateLimitRepository.increment(hourlyKey, 1L, TimeUnit.HOURS);

        // Then
        verify(redisTemplate, times(1)).expire(expectedHourlyKey, 1L, TimeUnit.HOURS);
    }

    @Test
    void getAttempts_WithInvalidNumberFormat_ShouldThrowException() {
        // Given
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(expectedRedisKey)).thenReturn("invalid");

        // When & Then
        assertThrows(NumberFormatException.class, 
                () -> rateLimitRepository.getAttempts(testKey));
    }

    @Test
    void increment_WithIPAddressKey_ShouldHandleCorrectly() {
        // Given
        String ipKey = "ip:192.168.1.1";
        String expectedIpKey = "rate_limit:ip:192.168.1.1";
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(expectedIpKey)).thenReturn(1L);

        // When
        Long result = rateLimitRepository.increment(ipKey, duration, timeUnit);

        // Then
        assertEquals(1L, result);
        verify(redisTemplate, times(1)).expire(expectedIpKey, duration, timeUnit);
    }

    @Test
    void reset_MultipleKeys_ShouldDeleteEachSeparately() {
        // Given
        String key1 = "user:1";
        String key2 = "user:2";

        // When
        rateLimitRepository.reset(key1);
        rateLimitRepository.reset(key2);

        // Then
        verify(redisTemplate, times(1)).delete("rate_limit:user:1");
        verify(redisTemplate, times(1)).delete("rate_limit:user:2");
    }

    @Test
    void getTimeToLive_WithKeyWithoutExpiration_ShouldReturnMinusOne() {
        // Given - Redis returns -1 for keys without expiration
        when(redisTemplate.getExpire(expectedRedisKey, TimeUnit.SECONDS)).thenReturn(-1L);

        // When
        Long result = rateLimitRepository.getTimeToLive(testKey);

        // Then
        assertEquals(-1L, result);
    }
}
