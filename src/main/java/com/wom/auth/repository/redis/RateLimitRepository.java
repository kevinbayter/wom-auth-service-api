package com.wom.auth.repository.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * Repository for managing rate limiting data in Redis.
 * Implements token bucket algorithm for rate limiting.
 */
@Repository
public class RateLimitRepository {

    private static final String RATE_LIMIT_PREFIX = "rate_limit:";
    
    private final RedisTemplate<String, String> redisTemplate;

    public RateLimitRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public Long increment(String key, long duration, TimeUnit timeUnit) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        Long count = redisTemplate.opsForValue().increment(redisKey);
        
        if (count != null && count == 1) {
            redisTemplate.expire(redisKey, duration, timeUnit);
        }
        
        return count;
    }

    public Long getAttempts(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        String value = redisTemplate.opsForValue().get(redisKey);
        return value != null ? Long.parseLong(value) : 0L;
    }

    public void reset(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        redisTemplate.delete(redisKey);
    }

    public Long getTimeToLive(String key) {
        String redisKey = RATE_LIMIT_PREFIX + key;
        return redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
    }
}
