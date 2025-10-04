package com.wom.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.security.KeyPair;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JwtService.
 */
class JwtServiceTest {

    private JwtService jwtService;
    private KeyPair keyPair;
    
    @BeforeEach
    void setUp() {
        // Generate test RSA key pair
        keyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", 900000L); // 15 min
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L); // 7 days
        ReflectionTestUtils.setField(jwtService, "privateKey", keyPair.getPrivate());
        ReflectionTestUtils.setField(jwtService, "publicKey", keyPair.getPublic());
    }

    @Test
    void generateAccessToken_WithValidData_ShouldReturnToken() {
        // Given
        Long userId = 1L;
        String username = "testuser";
        String email = "test@example.com";

        // When
        String token = jwtService.generateAccessToken(userId, username, email);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length); // JWT has 3 parts
    }

    @Test
    void generateRefreshToken_WithValidData_ShouldReturnToken() {
        // Given
        Long userId = 1L;
        String username = "testuser";

        // When
        String token = jwtService.generateRefreshToken(userId, username);

        // Then
        assertNotNull(token);
        assertFalse(token.isEmpty());
        assertEquals(3, token.split("\\.").length);
    }

    @Test
    void validateToken_WithValidAccessToken_ShouldReturnClaims() {
        // Given
        Long userId = 123L;
        String username = "testuser";
        String email = "test@example.com";
        String token = jwtService.generateAccessToken(userId, username, email);

        // When
        Claims claims = jwtService.validateToken(token);

        // Then
        assertNotNull(claims);
        assertEquals(userId, claims.get("userId", Long.class));
        assertEquals(username, claims.getSubject());
        assertEquals(email, claims.get("email", String.class));
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void validateToken_WithExpiredToken_ShouldThrowException() {
        // Given
        Date pastDate = new Date(System.currentTimeMillis() - 1000000);
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(pastDate)
                .setExpiration(pastDate)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        // When & Then
        assertThrows(Exception.class, () -> jwtService.validateToken(expiredToken));
    }

    @Test
    void validateToken_WithInvalidSignature_ShouldThrowException() {
        // Given
        KeyPair anotherKeyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        String tokenWithWrongSignature = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(anotherKeyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        // When & Then
        assertThrows(Exception.class, () -> jwtService.validateToken(tokenWithWrongSignature));
    }

    @Test
    void getUserIdFromToken_WithValidToken_ShouldReturnUserId() {
        // Given
        Long expectedUserId = 456L;
        String token = jwtService.generateAccessToken(expectedUserId, "testuser", "test@example.com");

        // When
        Long userId = jwtService.getUserIdFromToken(token);

        // Then
        assertNotNull(userId);
        assertEquals(expectedUserId, userId);
    }

    @Test
    void getUsernameFromToken_WithValidToken_ShouldReturnUsername() {
        // Given
        String expectedUsername = "testuser";
        String token = jwtService.generateAccessToken(1L, expectedUsername, "test@example.com");

        // When
        String username = jwtService.getUsernameFromToken(token);

        // Then
        assertNotNull(username);
        assertEquals(expectedUsername, username);
    }

    @Test
    void getTokenType_WithAccessToken_ShouldReturnAccess() {
        // Given
        String token = jwtService.generateAccessToken(1L, "testuser", "test@example.com");

        // When
        String tokenType = jwtService.getTokenType(token);

        // Then
        assertNotNull(tokenType);
        assertEquals("access", tokenType);
    }

    @Test
    void getTokenType_WithRefreshToken_ShouldReturnRefresh() {
        // Given
        String token = jwtService.generateRefreshToken(1L, "testuser");

        // When
        String tokenType = jwtService.getTokenType(token);

        // Then
        assertNotNull(tokenType);
        assertEquals("refresh", tokenType);
    }

    @Test
    void isTokenExpired_WithValidToken_ShouldReturnFalse() {
        // Given
        String token = jwtService.generateAccessToken(1L, "testuser", "test@example.com");

        // When
        boolean isExpired = jwtService.isTokenExpired(token);

        // Then
        assertFalse(isExpired);
    }

    @Test
    void isTokenExpired_WithExpiredToken_ShouldReturnTrue() {
        // Given
        Date pastDate = new Date(System.currentTimeMillis() - 1000000);
        String expiredToken = Jwts.builder()
                .setSubject("testuser")
                .setIssuedAt(pastDate)
                .setExpiration(pastDate)
                .signWith(keyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        // When
        boolean isExpired = jwtService.isTokenExpired(expiredToken);

        // Then
        assertTrue(isExpired);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid.token.format"})
    void isTokenExpired_WithInvalidTokens_ShouldReturnTrue(String invalidToken) {
        // When
        boolean isExpired = jwtService.isTokenExpired(invalidToken);

        // Then
        assertTrue(isExpired);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid.token.format", "not.a.valid.jwt.token", "header.payload"})
    void validateToken_WithInvalidTokens_ShouldThrowException(String invalidToken) {
        // When & Then
        assertThrows(Exception.class, () -> jwtService.validateToken(invalidToken));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid.token.format"})
    void getUserIdFromToken_WithInvalidTokens_ShouldThrowException(String invalidToken) {
        // When & Then
        assertThrows(Exception.class, () -> jwtService.getUserIdFromToken(invalidToken));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid.token.format"})
    void getUsernameFromToken_WithInvalidTokens_ShouldThrowException(String invalidToken) {
        // When & Then
        assertThrows(Exception.class, () -> jwtService.getUsernameFromToken(invalidToken));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid.token.format"})
    void getTokenType_WithInvalidTokens_ShouldThrowException(String invalidToken) {
        // When & Then
        assertThrows(Exception.class, () -> jwtService.getTokenType(invalidToken));
    }

    @Test
    void validateToken_WithRefreshToken_ShouldReturnClaims() {
        // Given
        Long userId = 789L;
        String username = "refreshuser";
        String token = jwtService.generateRefreshToken(userId, username);

        // When
        Claims claims = jwtService.validateToken(token);

        // Then
        assertNotNull(claims);
        assertEquals(userId, claims.get("userId", Long.class));
        assertEquals(username, claims.getSubject());
        assertEquals("refresh", claims.get("type", String.class));
    }

    @Test
    void generateAccessToken_ShouldContainExpirationTime() {
        // Given
        String token = jwtService.generateAccessToken(1L, "testuser", "test@example.com");

        // When
        Claims claims = jwtService.validateToken(token);

        // Then
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void generateRefreshToken_ShouldContainExpirationTime() {
        // Given
        String token = jwtService.generateRefreshToken(1L, "testuser");

        // When
        Claims claims = jwtService.validateToken(token);

        // Then
        assertNotNull(claims.getExpiration());
        assertTrue(claims.getExpiration().after(new Date()));
    }

    @Test
    void refreshToken_ShouldHaveLongerExpirationThanAccessToken() throws InterruptedException {
        // Given
        String accessToken = jwtService.generateAccessToken(1L, "testuser", "test@example.com");
        Thread.sleep(10); // Small delay to ensure different issued times
        String refreshToken = jwtService.generateRefreshToken(1L, "testuser");

        // When
        Claims accessClaims = jwtService.validateToken(accessToken);
        Claims refreshClaims = jwtService.validateToken(refreshToken);

        // Then
        assertTrue(refreshClaims.getExpiration().after(accessClaims.getExpiration()));
    }

    @Test
    void validateToken_WithTokenSignedByDifferentKey_ShouldThrowException() {
        // Given
        KeyPair differentKeyPair = Keys.keyPairFor(SignatureAlgorithm.RS256);
        String token = Jwts.builder()
                .setSubject("testuser")
                .claim("userId", 1L)
                .claim("type", "access")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(differentKeyPair.getPrivate(), SignatureAlgorithm.RS256)
                .compact();

        // When & Then
        assertThrows(Exception.class, () -> jwtService.validateToken(token));
    }
}
