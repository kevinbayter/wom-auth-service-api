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
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditService Tests")
class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditService auditService;

    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
    }

    @Test
    @DisplayName("Should log successful login attempt")
    void shouldLogSuccessfulLoginAttempt() {
        // Given
        Long userId = 1L;
        String identifier = "test@example.com";
        boolean success = true;
        String reason = null;
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(userId, identifier, success, reason, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.LOGIN_SUCCESS.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getIdentifier()).isEqualTo(identifier);
        assertThat(savedLog.getReason()).isNull();
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(savedLog.getUserAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
    }

    @Test
    @DisplayName("Should log failed login attempt")
    void shouldLogFailedLoginAttempt() {
        // Given
        Long userId = null;
        String identifier = "test@example.com";
        boolean success = false;
        String reason = "Invalid credentials";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(userId, identifier, success, reason, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isNull();
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.LOGIN_FAILURE.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.FAILURE.name());
        assertThat(savedLog.getIdentifier()).isEqualTo(identifier);
        assertThat(savedLog.getReason()).isEqualTo(reason);
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Should handle exception when saving login audit log")
    void shouldHandleExceptionWhenSavingLoginAuditLog() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should log single device logout")
    void shouldLogSingleDeviceLogout() {
        // Given
        Long userId = 1L;
        boolean allDevices = false;
        request.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)");

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLogout(userId, allDevices, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.LOGOUT.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(savedLog.getUserAgent()).isEqualTo("Mozilla/5.0 (Windows NT 10.0; Win64; x64)");
    }

    @Test
    @DisplayName("Should log all devices logout")
    void shouldLogAllDevicesLogout() {
        // Given
        Long userId = 1L;
        boolean allDevices = true;

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLogout(userId, allDevices, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.LOGOUT_ALL_DEVICES.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Should handle exception when saving logout audit log")
    void shouldHandleExceptionWhenSavingLogoutAuditLog() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        auditService.logLogout(1L, false, request);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should log successful refresh token")
    void shouldLogSuccessfulRefreshToken() {
        // Given
        Long userId = 1L;
        boolean success = true;
        String reason = null;

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logRefreshToken(userId, success, reason, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.REFRESH_TOKEN_SUCCESS.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getReason()).isNull();
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Should log failed refresh token")
    void shouldLogFailedRefreshToken() {
        // Given
        Long userId = 1L;
        boolean success = false;
        String reason = "Token expired";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logRefreshToken(userId, success, reason, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.REFRESH_TOKEN_FAILURE.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.FAILURE.name());
        assertThat(savedLog.getReason()).isEqualTo(reason);
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Should handle exception when saving refresh token audit log")
    void shouldHandleExceptionWhenSavingRefreshTokenAuditLog() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        auditService.logRefreshToken(1L, true, null, request);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should log account locked event")
    void shouldLogAccountLocked() {
        // Given
        Long userId = 1L;
        String identifier = "test@example.com";
        String reason = "Too many failed login attempts";

        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logAccountLocked(userId, identifier, reason, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        
        AuditLog savedLog = auditLogCaptor.getValue();
        assertThat(savedLog.getUserId()).isEqualTo(userId);
        assertThat(savedLog.getAction()).isEqualTo(AuditLog.Action.ACCOUNT_LOCKED.name());
        assertThat(savedLog.getResult()).isEqualTo(AuditLog.Result.SUCCESS.name());
        assertThat(savedLog.getIdentifier()).isEqualTo(identifier);
        assertThat(savedLog.getReason()).isEqualTo(reason);
        assertThat(savedLog.getIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Should handle exception when saving account locked audit log")
    void shouldHandleExceptionWhenSavingAccountLockedAuditLog() {
        // Given
        when(auditLogRepository.save(any(AuditLog.class)))
                .thenThrow(new RuntimeException("Database error"));

        // When & Then - should not throw exception
        auditService.logAccountLocked(1L, "test@example.com", "Too many attempts", request);

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("Should extract IP from X-Forwarded-For header")
    void shouldExtractIpFromXForwardedForHeader() {
        // Given
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getIpAddress()).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("Should extract IP from X-Real-IP header when X-Forwarded-For is absent")
    void shouldExtractIpFromXRealIpHeader() {
        // Given
        request.addHeader("X-Real-IP", "198.51.100.42");
        
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getIpAddress()).isEqualTo("198.51.100.42");
    }

    @Test
    @DisplayName("Should fallback to remote address when proxy headers are absent")
    void shouldFallbackToRemoteAddress() {
        // Given - request already has remoteAddr set in setUp()
        
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getIpAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @DisplayName("Should return UNKNOWN when request is null")
    void shouldReturnUnknownWhenRequestIsNull() {
        // Given
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, null);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getIpAddress()).isEqualTo("UNKNOWN");
        assertThat(auditLogCaptor.getValue().getUserAgent()).isNull();
    }

    @Test
    @DisplayName("Should extract User-Agent header")
    void shouldExtractUserAgentHeader() {
        // Given
        String userAgent = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7)";
        request.addHeader("User-Agent", userAgent);
        
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getUserAgent()).isEqualTo(userAgent);
    }

    @Test
    @DisplayName("Should truncate User-Agent if longer than 512 characters")
    void shouldTruncateUserAgentIfTooLong() {
        // Given
        String longUserAgent = "A".repeat(600);
        MockHttpServletRequest requestWithLongUA = new MockHttpServletRequest();
        requestWithLongUA.setRemoteAddr("192.168.1.100");
        requestWithLongUA.addHeader("User-Agent", longUserAgent);
        
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, requestWithLongUA);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getUserAgent()).hasSize(512);
        assertThat(auditLogCaptor.getValue().getUserAgent()).isEqualTo("A".repeat(512));
    }

    @Test
    @DisplayName("Should handle null User-Agent header")
    void shouldHandleNullUserAgentHeader() {
        // Given
        MockHttpServletRequest requestWithoutUA = new MockHttpServletRequest();
        requestWithoutUA.setRemoteAddr("192.168.1.100");
        
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, requestWithoutUA);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getUserAgent()).isNull();
    }

    @Test
    @DisplayName("Should handle X-Forwarded-For with single IP")
    void shouldHandleXForwardedForWithSingleIp() {
        // Given
        request.addHeader("X-Forwarded-For", "203.0.113.195");
        
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getIpAddress()).isEqualTo("203.0.113.195");
    }

    @Test
    @DisplayName("Should handle empty X-Forwarded-For header")
    void shouldHandleEmptyXForwardedForHeader() {
        // Given
        request.addHeader("X-Forwarded-For", "");
        request.addHeader("X-Real-IP", "198.51.100.42");
        
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getIpAddress()).isEqualTo("198.51.100.42");
    }

    @Test
    @DisplayName("Should handle empty X-Real-IP header and fallback to remote address")
    void shouldHandleEmptyXRealIpHeader() {
        // Given
        request.addHeader("X-Real-IP", "");
        
        ArgumentCaptor<AuditLog> auditLogCaptor = ArgumentCaptor.forClass(AuditLog.class);

        // When
        auditService.logLoginAttempt(1L, "test@example.com", true, null, request);

        // Then
        verify(auditLogRepository, times(1)).save(auditLogCaptor.capture());
        assertThat(auditLogCaptor.getValue().getIpAddress()).isEqualTo("192.168.1.100");
    }
}
