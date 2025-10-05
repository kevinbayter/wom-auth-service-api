package com.wom.auth.service;

import com.wom.auth.entity.AuditLog;
import com.wom.auth.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;

/**
 * Service for audit logging.
 * Records security and authentication events for compliance and analysis.
 * 
 * Uses async processing to avoid impacting authentication performance.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    /**
     * Log a login attempt.
     * Extracts request data on the calling thread before async execution.
     *
     * @param userId user ID (null if user not found)
     * @param identifier email or username used
     * @param success whether login was successful
     * @param reason failure reason (null if successful)
     * @param request HTTP request for IP and user agent
     */
    public void logLoginAttempt(
            Long userId,
            String identifier,
            boolean success,
            String reason,
            HttpServletRequest request
    ) {
        // Extract request data on the request thread (before async boundary)
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        
        // Pass immutable values to async method
        logLoginAttemptAsync(userId, identifier, success, reason, ipAddress, userAgent);
    }

    /**
     * Internal async method for login attempt logging.
     * Receives immutable extracted values instead of HttpServletRequest.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void logLoginAttemptAsync(
            Long userId,
            String identifier,
            boolean success,
            String reason,
            String ipAddress,
            String userAgent
    ) {
        try {
            String action = success ? AuditLog.Action.LOGIN_SUCCESS.name() : AuditLog.Action.LOGIN_FAILURE.name();
            String result = success ? AuditLog.Result.SUCCESS.name() : AuditLog.Result.FAILURE.name();

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .result(result)
                    .identifier(identifier)
                    .reason(reason)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);

            log.info("AUDIT_LOG | action={} | result={} | user_id={} | identifier={} | ip={} | reason={}",
                    action, result, userId, identifier, ipAddress, reason);
        } catch (Exception e) {
            log.error("Failed to save audit log for login attempt", e);
        }
    }

    /**
     * Log a logout event.
     * Extracts request data on the calling thread before async execution.
     *
     * @param userId user ID
     * @param allDevices whether logout was for all devices
     * @param request HTTP request
     */
    public void logLogout(Long userId, boolean allDevices, HttpServletRequest request) {
        // Extract request data on the request thread (before async boundary)
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        
        // Pass immutable values to async method
        logLogoutAsync(userId, allDevices, ipAddress, userAgent);
    }

    /**
     * Internal async method for logout logging.
     * Receives immutable extracted values instead of HttpServletRequest.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void logLogoutAsync(Long userId, boolean allDevices, String ipAddress, String userAgent) {
        try {
            String action = allDevices ? AuditLog.Action.LOGOUT_ALL_DEVICES.name() : AuditLog.Action.LOGOUT.name();

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .result(AuditLog.Result.SUCCESS.name())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);

            log.info("AUDIT_LOG | action={} | result=SUCCESS | user_id={} | ip={}",
                    action, userId, ipAddress);
        } catch (Exception e) {
            log.error("Failed to save audit log for logout", e);
        }
    }

    /**
     * Log a refresh token operation.
     * Extracts request data on the calling thread before async execution.
     *
     * @param userId user ID
     * @param success whether refresh was successful
     * @param reason failure reason (null if successful)
     * @param request HTTP request
     */
    public void logRefreshToken(
            Long userId,
            boolean success,
            String reason,
            HttpServletRequest request
    ) {
        // Extract request data on the request thread (before async boundary)
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        
        // Pass immutable values to async method
        logRefreshTokenAsync(userId, success, reason, ipAddress, userAgent);
    }

    /**
     * Internal async method for refresh token logging.
     * Receives immutable extracted values instead of HttpServletRequest.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void logRefreshTokenAsync(
            Long userId,
            boolean success,
            String reason,
            String ipAddress,
            String userAgent
    ) {
        try {
            String action = success ? AuditLog.Action.REFRESH_TOKEN_SUCCESS.name() : AuditLog.Action.REFRESH_TOKEN_FAILURE.name();
            String result = success ? AuditLog.Result.SUCCESS.name() : AuditLog.Result.FAILURE.name();

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .result(result)
                    .reason(reason)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);

            log.info("AUDIT_LOG | action={} | result={} | user_id={} | ip={} | reason={}",
                    action, result, userId, ipAddress, reason);
        } catch (Exception e) {
            log.error("Failed to save audit log for refresh token", e);
        }
    }

    /**
     * Log account locked event.
     * Extracts request data on the calling thread before async execution.
     *
     * @param userId user ID
     * @param identifier email or username
     * @param reason lock reason
     * @param request HTTP request
     */
    public void logAccountLocked(
            Long userId,
            String identifier,
            String reason,
            HttpServletRequest request
    ) {
        // Extract request data on the request thread (before async boundary)
        String ipAddress = getClientIp(request);
        String userAgent = getUserAgent(request);
        
        // Pass immutable values to async method
        logAccountLockedAsync(userId, identifier, reason, ipAddress, userAgent);
    }

    /**
     * Internal async method for account locked logging.
     * Receives immutable extracted values instead of HttpServletRequest.
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void logAccountLockedAsync(
            Long userId,
            String identifier,
            String reason,
            String ipAddress,
            String userAgent
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(AuditLog.Action.ACCOUNT_LOCKED.name())
                    .result(AuditLog.Result.SUCCESS.name())
                    .identifier(identifier)
                    .reason(reason)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();

            auditLogRepository.save(auditLog);

            log.warn("AUDIT_LOG | action=ACCOUNT_LOCKED | user_id={} | identifier={} | ip={} | reason={}",
                    userId, identifier, ipAddress, reason);
        } catch (Exception e) {
            log.error("Failed to save audit log for account locked", e);
        }
    }

    /**
     * Extract client IP from request.
     * Handles X-Forwarded-For header for proxied requests.
     *
     * @param request HTTP request
     * @return client IP address
     */
    private String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return "UNKNOWN";
        }

        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Extract User-Agent from request.
     *
     * @param request HTTP request
     * @return User-Agent string
     */
    private String getUserAgent(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        String userAgent = request.getHeader("User-Agent");
        if (userAgent != null && userAgent.length() > 512) {
            return userAgent.substring(0, 512);
        }
        return userAgent;
    }
}
