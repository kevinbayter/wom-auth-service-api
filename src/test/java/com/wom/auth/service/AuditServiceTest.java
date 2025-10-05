package com.wom.auth.service;

import com.wom.auth.entity.AuditLog;
import com.wom.auth.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Spy
    @InjectMocks
    private AuditService auditService;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
    }

    // ========== Login Attempt Tests ==========

    @Test
    @DisplayName("Should extract request data before async boundary for login attempt")
    void shouldExtractDataBeforeAsyncLoginAttempt() {
        // Given
        Long userId = 1L;
        String identifier = "test@example.com";
        boolean success = true;
        String reason = null;
        request.addHeader("User-Agent", "Mozilla/5.0");

        // When
        auditService.logLoginAttempt(userId, identifier, success, reason, request);

        // Then - verify async method called with extracted immutable values (not HttpServletRequest)
        verify(auditService, times(1)).logLoginAttemptAsync(
                eq(userId),
                eq(identifier),
                eq(success),
                eq(reason),
                eq("192.168.1.100"),
                eq("Mozilla/5.0")
        );
    }

    @Test
    @DisplayName("Should log successful login attempt")
    void shouldLogSuccessfulLoginAttempt() {
        // Given
        Long userId = 1L;
        String identifier = "test@example.com";
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64)";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttemptAsync(userId, identifier, true, null, ipAddress, userAgent);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.LOGIN_SUCCESS.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getIdentifier()).isEqualTo(identifier);
        assertThat(savedLog.getReason()).isNull();
        assertThat(savedLog.getIpAddress()).isEqualTo(ipAddress);
        assertThat(savedLog.getUserAgent()).isEqualTo(userAgent);
    }

    @Test
    @DisplayName("Should log failed login attempt")
    void shouldLogFailedLoginAttempt() {
        // Given
        Long userId = null;
        String identifier = "test@example.com";
        String reason = "Invalid credentials";
        String ipAddress = "192.168.1.100";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttemptAsync(userId, identifier, false, reason, ipAddress, null);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isNull();
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.LOGIN_FAILURE.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.FAILURE.name());
        assertThat(savedLog.getIdentifier()).isEqualTo(identifier);
        assertThat(savedLog.getReason()).isEqualTo(reason);
        assertThat(savedLog.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    @DisplayName("Should handle exception when saving login audit log")
    void shouldHandleExceptionWhenSavingLoginAuditLog() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        auditService.logLoginAttemptAsync(1L, "test@example.com", true, null, "192.168.1.100", null);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ========== Logout Tests ==========

    @Test
    @DisplayName("Should extract request data before async boundary for logout")
    void shouldExtractDataBeforeAsyncLogout() {
        // Given
        Long userId = 1L;
        request.addHeader("User-Agent", "Mozilla/5.0");

        // When
        auditService.logLogout(userId, false, request);

        // Then
        verify(auditService, times(1)).logLogoutAsync(
                eq(userId),
                eq(false),
                eq("192.168.1.100"),
                eq("Mozilla/5.0")
        );
    }

    @Test
    @DisplayName("Should log single device logout")
    void shouldLogSingleDeviceLogout() {
        // Given
        Long userId = 1L;
        String ipAddress = "192.168.1.100";
        String userAgent = "Mozilla/5.0";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLogoutAsync(userId, false, ipAddress, userAgent);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.LOGOUT.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getIpAddress()).isEqualTo(ipAddress);
        assertThat(savedLog.getUserAgent()).isEqualTo(userAgent);
    }

    @Test
    @DisplayName("Should log all devices logout")
    void shouldLogAllDevicesLogout() {
        // Given
        Long userId = 1L;
        String ipAddress = "192.168.1.100";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLogoutAsync(userId, true, ipAddress, null);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.LOGOUT_ALL_DEVICES.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    @DisplayName("Should handle exception when saving logout audit log")
    void shouldHandleExceptionWhenSavingLogoutAuditLog() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        auditService.logLogoutAsync(1L, false, "192.168.1.100", null);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ========== Refresh Token Tests ==========

    @Test
    @DisplayName("Should extract request data before async boundary for refresh token")
    void shouldExtractDataBeforeAsyncRefreshToken() {
        // Given
        Long userId = 1L;
        request.addHeader("User-Agent", "Mozilla/5.0");

        // When
        auditService.logRefreshToken(userId, true, null, request);

        // Then
        verify(auditService, times(1)).logRefreshTokenAsync(
                eq(userId),
                eq(true),
                eq(null),
                eq("192.168.1.100"),
                eq("Mozilla/5.0")
        );
    }

    @Test
    @DisplayName("Should log successful refresh token")
    void shouldLogSuccessfulRefreshToken() {
        // Given
        Long userId = 1L;
        String ipAddress = "192.168.1.100";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logRefreshTokenAsync(userId, true, null, ipAddress, null);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.REFRESH_TOKEN_SUCCESS.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getReason()).isNull();
        assertThat(savedLog.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    @DisplayName("Should log failed refresh token")
    void shouldLogFailedRefreshToken() {
        // Given
        Long userId = 1L;
        String reason = "Token expired";
        String ipAddress = "192.168.1.100";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logRefreshTokenAsync(userId, false, reason, ipAddress, null);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.REFRESH_TOKEN_FAILURE.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.FAILURE.name());
        assertThat(savedLog.getReason()).isEqualTo(reason);
        assertThat(savedLog.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    @DisplayName("Should handle exception when saving refresh token audit log")
    void shouldHandleExceptionWhenSavingRefreshTokenAuditLog() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        auditService.logRefreshTokenAsync(1L, true, null, "192.168.1.100", null);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ========== Account Locked Tests ==========

    @Test
    @DisplayName("Should extract request data before async boundary for account locked")
    void shouldExtractDataBeforeAsyncAccountLocked() {
        // Given
        Long userId = 1L;
        String identifier = "test@example.com";
        String reason = "Too many attempts";
        request.addHeader("User-Agent", "Mozilla/5.0");

        // When
        auditService.logAccountLocked(userId, identifier, reason, request);

        // Then
        verify(auditService, times(1)).logAccountLockedAsync(
                eq(userId),
                eq(identifier),
                eq(reason),
                eq("192.168.1.100"),
                eq("Mozilla/5.0")
        );
    }

    @Test
    @DisplayName("Should log account locked event")
    void shouldLogAccountLocked() {
        // Given
        Long userId = 1L;
        String identifier = "test@example.com";
        String reason = "Too many failed login attempts";
        String ipAddress = "192.168.1.100";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logAccountLockedAsync(userId, identifier, reason, ipAddress, null);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.ACCOUNT_LOCKED.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getIdentifier()).isEqualTo(identifier);
        assertThat(savedLog.getReason()).isEqualTo(reason);
        assertThat(savedLog.getIpAddress()).isEqualTo(ipAddress);
    }

    @Test
    @DisplayName("Should handle exception when saving account locked audit log")
    void shouldHandleExceptionWhenSavingAccountLockedAuditLog() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        auditService.logAccountLockedAsync(1L, "test@example.com", "Too many attempts", "192.168.1.100", null);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // ========== IP Extraction Tests ==========

    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header")
    void shouldExtractIpFromXForwardedForHeader() {
        // Given
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(),
                eq("203.0.113.195"),  // First IP from X-Forwarded-For
                any()
        );
    }

    @Test
    @DisplayName("Should extract IP from X-Real-IP header when X-Forwarded-For is absent")
    void shouldExtractIpFromXRealIpHeader() {
        // Given
        request.addHeader("X-Real-IP", "198.51.100.42");

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(),
                eq("198.51.100.42"),
                any()
        );
    }

    @Test
    @DisplayName("Should fallback to remote address when proxy headers are absent")
    void shouldFallbackToRemoteAddress() {
        // Given - request already has remoteAddr set in setUp()

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(),
                eq("192.168.1.100"),
                any()
        );
    }

    @Test
    @DisplayName("Should return UNKNOWN when request is null")
    void shouldReturnUnknownWhenRequestIsNull() {
        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, null);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(),
                eq("UNKNOWN"),
                eq(null)
        );
    }

    @Test
    @DisplayName("Should handle X-Forwarded-For with single IP")
    void shouldHandleXForwardedForWithSingleIp() {
        // Given
        request.addHeader("X-Forwarded-For", "203.0.113.195");

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(),
                eq("203.0.113.195"),
                any()
        );
    }

    @Test
    @DisplayName("Should handle empty X-Forwarded-For header")
    void shouldHandleEmptyXForwardedForHeader() {
        // Given
        request.addHeader("X-Forwarded-For", "");
        request.addHeader("X-Real-IP", "198.51.100.42");

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(),
                eq("198.51.100.42"),
                any()
        );
    }

    @Test
    @DisplayName("Should handle empty X-Real-IP header and fallback to remote address")
    void shouldHandleEmptyXRealIpHeader() {
        // Given
        request.addHeader("X-Real-IP", "");

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(),
                eq("192.168.1.100"),
                any()
        );
    }

    // ========== User-Agent Extraction Tests ==========

    @Test
    @DisplayName("Should extract User-Agent header")
    void shouldExtractUserAgentHeader() {
        // Given
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
        request.addHeader("User-Agent", userAgent);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(), any(),
                eq(userAgent)
        );
    }

    @Test
    @DisplayName("Should truncate User-Agent if longer than 512 characters")
    void shouldTruncateUserAgentIfTooLong() {
        // Given
        String longUserAgent = "A".repeat(600);
        MockHttpServletRequest requestWithLongUA = new MockHttpServletRequest();
        requestWithLongUA.setRemoteAddr("192.168.1.100");
        requestWithLongUA.addHeader("User-Agent", longUserAgent);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, requestWithLongUA);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(), any(),
                eq("A".repeat(512))
        );
    }

    @Test
    @DisplayName("Should handle null User-Agent header")
    void shouldHandleNullUserAgentHeader() {
        // Given - MockHttpServletRequest without User-Agent header
        MockHttpServletRequest requestWithoutUA = new MockHttpServletRequest();
        requestWithoutUA.setRemoteAddr("192.168.1.100");

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, requestWithoutUA);

        // Then
        verify(auditService).logLoginAttemptAsync(
                any(), any(), anyBoolean(), any(), any(),
                eq(null)
        );
    }
}
