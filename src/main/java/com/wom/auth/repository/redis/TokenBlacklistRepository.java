package com.wom.auth.repository.redis;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.concurrent.TimeUnit;

/**
 * Repository for managing token blacklist in Redis.
 * Used to invalidate JWT access tokens before their natural expiration.
 *
 * @author Kevin Bayter
 */
@Repository
public class TokenBlacklistRepository {

    private static final String BLACKLIST_PREFIX = "blacklist:token:";
    
    private final RedisTemplate<String, String> redisTemplate;

    public TokenBlacklistRepository(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void blacklistToken(String tokenId, long expirationSeconds) {
        String key = BLACKLIST_PREFIX + tokenId;
        redisTemplate.opsForValue().set(key, "blacklisted", expirationSeconds, TimeUnit.SECONDS);
    }

    public boolean isTokenBlacklisted(String tokenId) {
        String key = BLACKLIST_PREFIX + tokenId;
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void removeFromBlacklist(String tokenId) {
        String key = BLACKLIST_PREFIX + tokenId;
        redisTemplate.delete(key);
    }
}
