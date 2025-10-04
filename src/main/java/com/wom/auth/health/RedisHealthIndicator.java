package com.wom.auth.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Custom health indicator for Redis connectivity.
 * 
 * Performs an active health check by executing a PING command
 * to verify that Redis is accessible and responsive.
 * 
 * This is useful for monitoring the health of the session store
 * and cache layer used for refresh tokens and rate limiting.
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private static final String PING_RESPONSE = "PONG";
    private static final int TIMEOUT_MS = 3000;
    
    private final RedisTemplate<String, Object> redisTemplate;

    public RedisHealthIndicator(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            // Execute PING command
            var connectionFactory = redisTemplate.getConnectionFactory();
            if (connectionFactory == null) {
                return Health.down()
                        .withDetail("cache", "Redis")
                        .withDetail("error", "ConnectionFactory is null")
                        .build();
            }
            
            String response = connectionFactory.getConnection().ping();
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (PING_RESPONSE.equals(response)) {
                return Health.up()
                        .withDetail("cache", "Redis")
                        .withDetail("status", "UP")
                        .withDetail("responseTime", responseTime + "ms")
                        .withDetail("ping", response)
                        .build();
            } else {
                return Health.down()
                        .withDetail("cache", "Redis")
                        .withDetail("reason", "Unexpected ping response: " + response)
                        .build();
            }
        } catch (Exception e) {
            return Health.down()
                    .withDetail("cache", "Redis")
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
