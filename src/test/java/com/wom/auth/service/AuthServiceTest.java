package com.wom.auth.service;

import com.wom.auth.dto.LoginResponse;
import com.wom.auth.entity.RefreshToken;
import com.wom.auth.entity.User;
import com.wom.auth.exception.AccountLockedException;
import com.wom.auth.exception.InvalidCredentialsException;
import com.wom.auth.metrics.MetricsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import javax.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link AuthService}.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserService userService;

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @Mock
    private MetricsService metricsService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RefreshToken testRefreshToken;
    private HttpServletRequest mockRequest;
    private final String testEmail = "test@example.com";
    private final String testPassword = "password123";
    private final String testAccessToken = "access.token.jwt";
    private final String testRefreshTokenJwt = "refresh.token.jwt";
    private final Long testUserId = 1L;
    private final String testUsername = "testuser";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "accessTokenExpiration", 900000L);
        
        // Create mock HTTP request
        mockRequest = new MockHttpServletRequest();
        ((MockHttpServletRequest) mockRequest).setRemoteAddr("127.0.0.1");
        ((MockHttpServletRequest) mockRequest).addHeader("User-Agent", "Test User Agent");
        
        testUser = User.builder()
                .id(testUserId)
                .email(testEmail)
                .username(testUsername)
                .passwordHash("$2a$10$hashedPassword")
                .status(User.UserStatus.ACTIVE)
                .failedAttempts(0)
                .build();

        testRefreshToken = RefreshToken.builder()
                .id(1L)
                .userId(testUserId)
                .tokenHash("hashedToken")
                .expiresAt(LocalDateTime.now().plusDays(7))
                .build();
        
        // Setup MetricsService mocks to execute the operation (lenient for flexibility)
        lenient().when(metricsService.recordLoginOperation(any())).thenAnswer(invocation -> {
            MetricsService.LoginOperation<?> operation = invocation.getArgument(0);
            return operation.execute();
        });
        
        lenient().when(metricsService.recordRefreshOperation(any())).thenAnswer(invocation -> {
            MetricsService.RefreshOperation<?> operation = invocation.getArgument(0);
            return operation.execute();
        });
    }

    @Test
    void authenticate_WithValidCredentials_ShouldReturnTokens() {
        // Arrange
        when(userService.findByEmailOrUsername(testEmail)).thenReturn(Optional.of(testUser));
        when(userService.isAccountLocked(testUser)).thenReturn(false);
        when(userService.isAccountActive(testUser)).thenReturn(true);
        when(userService.validatePassword(testPassword, testUser.getPasswordHash())).thenReturn(true);
        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn(testAccessToken);
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn(testRefreshTokenJwt);
        when(tokenService.createRefreshToken(anyLong(), anyString())).thenReturn(testRefreshToken);

        // Act
        LoginResponse response = authService.authenticate(testEmail, testPassword, mockRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testAccessToken, response.getAccessToken());
        assertEquals(testRefreshTokenJwt, response.getRefreshToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(900L, response.getExpiresIn());
        verify(userService, times(1)).resetFailedAttempts(testUser);
        verify(userService, times(1)).updateLastLogin(testUser);
    }

    @Test
    void authenticate_WithNonExistingUser_ShouldThrowException() {
        // Arrange
        when(userService.findByEmailOrUsername(testEmail)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, 
                () -> authService.authenticate(testEmail, testPassword, mockRequest));
        verify(userService, never()).validatePassword(anyString(), anyString());
    }

    @Test
    void authenticate_WithLockedAccount_ShouldThrowException() {
        // Arrange
        testUser.lockAccount(30);
        when(userService.findByEmailOrUsername(testEmail)).thenReturn(Optional.of(testUser));
        when(userService.isAccountLocked(testUser)).thenReturn(true);

        // Act & Assert
        assertThrows(AccountLockedException.class, 
                () -> authService.authenticate(testEmail, testPassword, mockRequest));
        verify(userService, never()).validatePassword(anyString(), anyString());
    }

    @Test
    void authenticate_WithInactiveAccount_ShouldThrowException() {
        // Arrange
        testUser.setStatus(User.UserStatus.INACTIVE);
        when(userService.findByEmailOrUsername(testEmail)).thenReturn(Optional.of(testUser));
        when(userService.isAccountLocked(testUser)).thenReturn(false);
        when(userService.isAccountActive(testUser)).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, 
                () -> authService.authenticate(testEmail, testPassword, mockRequest));
        verify(userService, never()).validatePassword(anyString(), anyString());
    }

    @Test
    void authenticate_WithInvalidPassword_ShouldThrowExceptionAndIncrementFailedAttempts() {
        // Arrange
        when(userService.findByEmailOrUsername(testEmail)).thenReturn(Optional.of(testUser));
        when(userService.isAccountLocked(testUser)).thenReturn(false);
        when(userService.isAccountActive(testUser)).thenReturn(true);
        when(userService.validatePassword(testPassword, testUser.getPasswordHash())).thenReturn(false);

        // Act & Assert
        assertThrows(InvalidCredentialsException.class, 
                () -> authService.authenticate(testEmail, testPassword, mockRequest));
        verify(userService, times(1)).incrementFailedAttempts(testUser);
        verify(userService, never()).resetFailedAttempts(testUser);
    }

    @Test
    void refreshAccessToken_WithValidToken_ShouldReturnNewTokens() {
        // Arrange
        Claims mockClaims = new DefaultClaims();
        when(tokenService.validateRefreshToken(testRefreshTokenJwt)).thenReturn(Optional.of(testRefreshToken));
        when(jwtService.validateToken(testRefreshTokenJwt)).thenReturn(mockClaims);
        when(jwtService.getUserIdFromToken(testRefreshTokenJwt)).thenReturn(testUserId);
        when(userService.findById(testUserId)).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(anyLong(), anyString(), anyString())).thenReturn(testAccessToken);
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn("new.refresh.token");
        when(tokenService.createRefreshToken(anyLong(), anyString())).thenReturn(testRefreshToken);

        // Act
        LoginResponse response = authService.refreshAccessToken(testRefreshTokenJwt, mockRequest);

        // Assert
        assertNotNull(response);
        assertEquals(testAccessToken, response.getAccessToken());
        assertEquals("new.refresh.token", response.getRefreshToken());
        verify(tokenService, times(1)).revokeRefreshToken(testRefreshTokenJwt);
        verify(tokenService, times(1)).createRefreshToken(anyLong(), anyString());
    }

    @Test
    void refreshAccessToken_WithInvalidToken_ShouldThrowException() {
        // Arrange
        when(tokenService.validateRefreshToken(testRefreshTokenJwt)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> authService.refreshAccessToken(testRefreshTokenJwt, mockRequest));
        verify(jwtService, never()).generateAccessToken(anyLong(), anyString(), anyString());
    }

    @Test
    void refreshAccessToken_WithNonExistingUser_ShouldThrowException() {
        // Arrange
        Claims mockClaims = new DefaultClaims();
        when(tokenService.validateRefreshToken(testRefreshTokenJwt)).thenReturn(Optional.of(testRefreshToken));
        when(jwtService.validateToken(testRefreshTokenJwt)).thenReturn(mockClaims);
        when(jwtService.getUserIdFromToken(testRefreshTokenJwt)).thenReturn(testUserId);
        when(userService.findById(testUserId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(IllegalArgumentException.class, 
                () -> authService.refreshAccessToken(testRefreshTokenJwt, mockRequest));
        verify(jwtService, never()).generateAccessToken(anyLong(), anyString(), anyString());
    }

    @Test
    void logout_WithValidToken_ShouldBlacklistTokenAndRevokeRefresh() {
        // Arrange
        when(jwtService.isTokenExpired(testAccessToken)).thenReturn(false);
        when(jwtService.getUserIdFromToken(testAccessToken)).thenReturn(testUserId);
        when(jwtService.getUsernameFromToken(testAccessToken)).thenReturn(testUsername);
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn(testRefreshTokenJwt);

        // Act
        authService.logout(testAccessToken, mockRequest);

        // Assert
        verify(tokenService, times(1)).blacklistAccessToken(eq(testAccessToken), anyLong());
        verify(tokenService, times(1)).revokeRefreshToken(testRefreshTokenJwt);
    }

    @Test
    void logout_WithExpiredToken_ShouldNotBlacklistButRevokeRefresh() {
        // Arrange
        when(jwtService.isTokenExpired(testAccessToken)).thenReturn(true);
        when(jwtService.getUserIdFromToken(testAccessToken)).thenReturn(testUserId);
        when(jwtService.getUsernameFromToken(testAccessToken)).thenReturn(testUsername);
        when(jwtService.generateRefreshToken(anyLong(), anyString())).thenReturn(testRefreshTokenJwt);

        // Act
        authService.logout(testAccessToken, mockRequest);

        // Assert
        verify(tokenService, never()).blacklistAccessToken(anyString(), anyLong());
        verify(tokenService, times(1)).revokeRefreshToken(testRefreshTokenJwt);
    }

    @Test
    void logout_WithNullToken_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> authService.logout(null, mockRequest));
        verify(tokenService, never()).blacklistAccessToken(anyString(), anyLong());
    }

    @Test
    void logout_WithEmptyToken_ShouldNotThrowException() {
        // Act & Assert
        assertDoesNotThrow(() -> authService.logout("", mockRequest));
        verify(tokenService, never()).blacklistAccessToken(anyString(), anyLong());
    }

    @Test
    void logoutAllDevices_WithValidToken_ShouldRevokeAllTokens() {
        // Arrange
        when(jwtService.getUserIdFromToken(testAccessToken)).thenReturn(testUserId);
        when(jwtService.isTokenExpired(testAccessToken)).thenReturn(false);

        // Act
        authService.logoutAllDevices(testAccessToken, mockRequest);

        // Assert
        verify(tokenService, times(1)).revokeAllUserTokens(testUserId);
        verify(tokenService, times(1)).blacklistAccessToken(eq(testAccessToken), anyLong());
    }

    @Test
    void logoutAllDevices_WithExpiredToken_ShouldRevokeAllButNotBlacklist() {
        // Arrange
        when(jwtService.getUserIdFromToken(testAccessToken)).thenReturn(testUserId);
        when(jwtService.isTokenExpired(testAccessToken)).thenReturn(true);

        // Act
        authService.logoutAllDevices(testAccessToken, mockRequest);

        // Assert
        verify(tokenService, times(1)).revokeAllUserTokens(testUserId);
        verify(tokenService, never()).blacklistAccessToken(anyString(), anyLong());
    }
}
