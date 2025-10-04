package com.wom.auth.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RedisHealthIndicator.
 * Tests Redis health check functionality including success, failure, and edge cases.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RedisHealthIndicator Tests")
class RedisHealthIndicatorTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private RedisConnectionFactory connectionFactory;

    @Mock
    private RedisConnection redisConnection;

    private RedisHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new RedisHealthIndicator(redisTemplate);
    }

    @Test
    @DisplayName("Should return UP status when Redis is healthy")
    void shouldReturnUpWhenRedisIsHealthy() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsKey("cache")
                .containsKey("status")
                .containsKey("responseTime")
                .containsKey("ping");
        assertThat(health.getDetails().get("cache")).isEqualTo("Redis");
        assertThat(health.getDetails().get("status")).isEqualTo("UP");
        assertThat(health.getDetails().get("ping")).isEqualTo("PONG");

        verify(redisTemplate).getConnectionFactory();
        verify(connectionFactory).getConnection();
        verify(redisConnection).ping();
    }

    @Test
    @DisplayName("Should return DOWN status when connection factory is null")
    void shouldReturnDownWhenConnectionFactoryIsNull() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(null);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsKey("cache")
                .containsKey("error");
        assertThat(health.getDetails().get("cache")).isEqualTo("Redis");
        assertThat(health.getDetails().get("error")).asString().contains("ConnectionFactory is null");

        verify(redisTemplate).getConnectionFactory();
        verify(connectionFactory, never()).getConnection();
    }

    @Test
    @DisplayName("Should return DOWN status when ping fails")
    void shouldReturnDownWhenPingFails() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("ERROR");

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsKey("cache")
                .containsKey("reason");
        assertThat(health.getDetails().get("cache")).isEqualTo("Redis");
        assertThat(health.getDetails().get("reason")).asString().contains("Unexpected ping response");
    }

    @Test
    @DisplayName("Should return DOWN status when exception occurs")
    void shouldReturnDownWhenExceptionOccurs() {
        // Given
        RuntimeException exception = new RuntimeException("Connection timeout");
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenThrow(exception);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsKey("cache")
                .containsKey("error")
                .containsKey("message");
        assertThat(health.getDetails().get("cache")).isEqualTo("Redis");
        assertThat(health.getDetails().get("error")).isEqualTo("RuntimeException");
        assertThat(health.getDetails().get("message")).asString().contains("Connection timeout");

        verify(redisTemplate).getConnectionFactory();
        verify(connectionFactory).getConnection();
    }

    @Test
    @DisplayName("Should measure response time accurately")
    void shouldMeasureResponseTimeAccurately() throws Exception {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenAnswer(invocation -> {
            Thread.sleep(50); // Simulate delay
            return "PONG";
        });

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        Object responseTime = health.getDetails().get("responseTime");
        assertThat(responseTime).isNotNull();
        assertThat(responseTime.toString()).matches("\\d+ms");
        
        // Response time should be at least 50ms
        String timeStr = responseTime.toString().replace("ms", "");
        long time = Long.parseLong(timeStr);
        assertThat(time).isGreaterThanOrEqualTo(50L);
    }

    @Test
    @DisplayName("Should handle null ping response")
    void shouldHandleNullPingResponse() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn(null);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("reason");
        assertThat(health.getDetails().get("reason")).asString().contains("Unexpected ping response");
    }

    @Test
    @DisplayName("Should handle empty ping response")
    void shouldHandleEmptyPingResponse() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("");

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsKey("cache")
                .containsKey("reason");
        assertThat(health.getDetails().get("reason")).asString().contains("Unexpected ping response");
    }

    @Test
    @DisplayName("Should include all required details in UP status")
    void shouldIncludeAllRequiredDetailsInUpStatus() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("PONG");

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getDetails()).hasSize(4); // cache, status, responseTime, ping
        assertThat(health.getDetails())
                .containsKey("cache")
                .containsKey("status")
                .containsKey("responseTime")
                .containsKey("ping");
    }

    @Test
    @DisplayName("Should include required details in DOWN status with error")
    void shouldIncludeRequiredDetailsInDownStatusWithError() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(null);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getDetails()).hasSize(2); // cache, error
        assertThat(health.getDetails())
                .containsKey("cache")
                .containsKey("error");
    }

    @Test
    @DisplayName("Should validate PONG response case-sensitively")
    void shouldValidatePongResponseCaseSensitively() {
        // Given
        when(redisTemplate.getConnectionFactory()).thenReturn(connectionFactory);
        when(connectionFactory.getConnection()).thenReturn(redisConnection);
        when(redisConnection.ping()).thenReturn("pong"); // lowercase

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails()).containsKey("reason");
        assertThat(health.getDetails().get("reason")).asString().contains("Unexpected ping response");
    }
}
