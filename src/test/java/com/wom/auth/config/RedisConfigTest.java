package com.wom.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RedisConfig.
 * 
 * @author Kevin Bayter
 * @see <a href="https://github.com/kevinbayter">GitHub Profile</a>
 */
class RedisConfigTest {

    private RedisConfig redisConfig;

    @Test
    void redisConnectionFactory_WithHostAndPort_ShouldCreateFactory() {
        // Given
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "");

        // When
        LettuceConnectionFactory factory = redisConfig.redisConnectionFactory();

        // Then
        assertNotNull(factory);
        RedisStandaloneConfiguration config = factory.getStandaloneConfiguration();
        assertEquals("localhost", config.getHostName());
        assertEquals(6379, config.getPort());
    }

    @Test
    void redisConnectionFactory_WithPassword_ShouldSetPassword() {
        // Given
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "redis.example.com");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6380);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "secretPassword");

        // When
        LettuceConnectionFactory factory = redisConfig.redisConnectionFactory();

        // Then
        assertNotNull(factory);
        RedisStandaloneConfiguration config = factory.getStandaloneConfiguration();
        assertTrue(config.getPassword().isPresent());
        assertEquals("secretPassword", new String(config.getPassword().get()));
    }

    @Test
    void redisConnectionFactory_WithEmptyPassword_ShouldNotSetPassword() {
        // Given
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "");

        // When
        LettuceConnectionFactory factory = redisConfig.redisConnectionFactory();

        // Then
        assertNotNull(factory);
        RedisStandaloneConfiguration config = factory.getStandaloneConfiguration();
        assertFalse(config.getPassword().isPresent());
    }

    @Test
    void redisConnectionFactory_WithNullPassword_ShouldNotSetPassword() {
        // Given
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", null);

        // When
        LettuceConnectionFactory factory = redisConfig.redisConnectionFactory();

        // Then
        assertNotNull(factory);
        RedisStandaloneConfiguration config = factory.getStandaloneConfiguration();
        assertFalse(config.getPassword().isPresent());
    }

    @Test
    void redisConnectionFactory_WithCustomPort_ShouldUseCustomPort() {
        // Given
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 7000);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "");

        // When
        LettuceConnectionFactory factory = redisConfig.redisConnectionFactory();

        // Then
        assertNotNull(factory);
        RedisStandaloneConfiguration config = factory.getStandaloneConfiguration();
        assertEquals(7000, config.getPort());
    }

    @Test
    void redisConnectionFactory_WithRemoteHost_ShouldUseRemoteHost() {
        // Given
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "prod-redis.example.com");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "prodPassword");

        // When
        LettuceConnectionFactory factory = redisConfig.redisConnectionFactory();

        // Then
        assertNotNull(factory);
        RedisStandaloneConfiguration config = factory.getStandaloneConfiguration();
        assertEquals("prod-redis.example.com", config.getHostName());
        assertTrue(config.getPassword().isPresent());
    }

    @Test
    void redisTemplate_WithConnectionFactory_ShouldCreateTemplate() {
        // Given
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "");
        
        LettuceConnectionFactory connectionFactory = redisConfig.redisConnectionFactory();

        // When
        RedisTemplate<String, String> template = redisConfig.redisTemplate(connectionFactory);

        // Then
        assertNotNull(template);
        assertEquals(connectionFactory, template.getConnectionFactory());
        assertNotNull(template.getKeySerializer());
        assertNotNull(template.getValueSerializer());
        assertNotNull(template.getHashKeySerializer());
        assertNotNull(template.getHashValueSerializer());
    }

    @Test
    void redisTemplate_ShouldHaveStringSerializers() {
        // Given
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "");
        
        LettuceConnectionFactory connectionFactory = redisConfig.redisConnectionFactory();

        // When
        RedisTemplate<String, String> template = redisConfig.redisTemplate(connectionFactory);

        // Then
        assertNotNull(template.getKeySerializer());
        assertNotNull(template.getValueSerializer());
        assertEquals("StringRedisSerializer", template.getKeySerializer().getClass().getSimpleName());
        assertEquals("StringRedisSerializer", template.getValueSerializer().getClass().getSimpleName());
    }

    @Test
    void redisTemplate_ShouldHaveHashSerializers() {
        // Given
        redisConfig = new RedisConfig();
        ReflectionTestUtils.setField(redisConfig, "redisHost", "localhost");
        ReflectionTestUtils.setField(redisConfig, "redisPort", 6379);
        ReflectionTestUtils.setField(redisConfig, "redisPassword", "");
        
        LettuceConnectionFactory connectionFactory = redisConfig.redisConnectionFactory();

        // When
        RedisTemplate<String, String> template = redisConfig.redisTemplate(connectionFactory);

        // Then
        assertNotNull(template.getHashKeySerializer());
        assertNotNull(template.getHashValueSerializer());
        assertEquals("StringRedisSerializer", template.getHashKeySerializer().getClass().getSimpleName());
        assertEquals("StringRedisSerializer", template.getHashValueSerializer().getClass().getSimpleName());
    }

    @Test
    void redisConnectionFactory_MultipleInstances_ShouldCreateIndependentFactories() {
        // Given
        RedisConfig config1 = new RedisConfig();
        RedisConfig config2 = new RedisConfig();
        
        ReflectionTestUtils.setField(config1, "redisHost", "localhost");
        ReflectionTestUtils.setField(config1, "redisPort", 6379);
        ReflectionTestUtils.setField(config1, "redisPassword", "");
        
        ReflectionTestUtils.setField(config2, "redisHost", "remote-host");
        ReflectionTestUtils.setField(config2, "redisPort", 6380);
        ReflectionTestUtils.setField(config2, "redisPassword", "password");

        // When
        LettuceConnectionFactory factory1 = config1.redisConnectionFactory();
        LettuceConnectionFactory factory2 = config2.redisConnectionFactory();

        // Then
        assertNotEquals(factory1, factory2);
        assertEquals("localhost", factory1.getStandaloneConfiguration().getHostName());
        assertEquals("remote-host", factory2.getStandaloneConfiguration().getHostName());
    }
}
