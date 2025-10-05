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
     *
     * @param userId user ID (null if user not found)
     * @param identifier email or username used
     * @param success whether login was successful
     * @param reason failure reason (null if successful)
     * @param request HTTP request for IP and user agent
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLoginAttempt(
            Long userId,
            String identifier,
            boolean success,
            String reason,
            HttpServletRequest request
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
                    .ipAddress(getClientIp(request))
                    .userAgent(getUserAgent(request))
                    .build();

            auditLogRepository.save(auditLog);

            log.info("AUDIT_LOG | action={} | result={} | user_id={} | identifier={} | ip={} | reason={}",
                    action, result, userId, identifier, getClientIp(request), reason);
        } catch (Exception e) {
            log.error("Failed to save audit log for login attempt", e);
        }
    }

    /**
     * Log a logout event.
     *
     * @param userId user ID
     * @param allDevices whether logout was for all devices
     * @param request HTTP request
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logLogout(Long userId, boolean allDevices, HttpServletRequest request) {
        try {
            String action = allDevices ? AuditLog.Action.LOGOUT_ALL_DEVICES.name() : AuditLog.Action.LOGOUT.name();

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .result(AuditLog.Result.SUCCESS.name())
                    .ipAddress(getClientIp(request))
                    .userAgent(getUserAgent(request))
                    .build();

            auditLogRepository.save(auditLog);

            log.info("AUDIT_LOG | action={} | result=SUCCESS | user_id={} | ip={}",
                    action, userId, getClientIp(request));
        } catch (Exception e) {
            log.error("Failed to save audit log for logout", e);
        }
    }

    /**
     * Log a refresh token operation.
     *
     * @param userId user ID
     * @param success whether refresh was successful
     * @param reason failure reason (null if successful)
     * @param request HTTP request
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logRefreshToken(
            Long userId,
            boolean success,
            String reason,
            HttpServletRequest request
    ) {
        try {
            String action = success ? AuditLog.Action.REFRESH_TOKEN_SUCCESS.name() : AuditLog.Action.REFRESH_TOKEN_FAILURE.name();
            String result = success ? AuditLog.Result.SUCCESS.name() : AuditLog.Result.FAILURE.name();

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .result(result)
                    .reason(reason)
                    .ipAddress(getClientIp(request))
                    .userAgent(getUserAgent(request))
                    .build();

            auditLogRepository.save(auditLog);

            log.info("AUDIT_LOG | action={} | result={} | user_id={} | ip={} | reason={}",
                    action, result, userId, getClientIp(request), reason);
        } catch (Exception e) {
            log.error("Failed to save audit log for refresh token", e);
        }
    }

    /**
     * Log account locked event.
     *
     * @param userId user ID
     * @param identifier email or username
     * @param reason lock reason
     * @param request HTTP request
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void logAccountLocked(
            Long userId,
            String identifier,
            String reason,
            HttpServletRequest request
    ) {
        try {
            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(AuditLog.Action.ACCOUNT_LOCKED.name())
                    .result(AuditLog.Result.SUCCESS.name())
                    .identifier(identifier)
                    .reason(reason)
                    .ipAddress(getClientIp(request))
                    .userAgent(getUserAgent(request))
                    .build();

            auditLogRepository.save(auditLog);

            log.warn("AUDIT_LOG | action=ACCOUNT_LOCKED | user_id={} | identifier={} | ip={} | reason={}",
                    userId, identifier, getClientIp(request), reason);
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
