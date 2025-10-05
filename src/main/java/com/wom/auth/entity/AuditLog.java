package com.wom.auth.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * AuditLog entity for tracking security and authentication events.
 * Records all login attempts, token operations, and security-related actions.
 */
@Entity
@Table(name = "audit_log")
@EntityListeners(AuditingEntityListener.class)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id")
    private Long userId;

    @NotBlank
    @Size(max = 50)
    @Column(nullable = false, length = 50)
    private String action;

    @NotBlank
    @Size(max = 20)
    @Column(nullable = false, length = 20)
    private String result;

    @Size(max = 45)
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Size(max = 512)
    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Size(max = 100)
    @Column(length = 100)
    private String identifier;

    @Size(max = 255)
    @Column(length = 255)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String details;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Audit action types
     */
    public enum Action {
        LOGIN_ATTEMPT,
        LOGIN_SUCCESS,
        LOGIN_FAILURE,
        LOGOUT,
        LOGOUT_ALL_DEVICES,
        REFRESH_TOKEN_SUCCESS,
        REFRESH_TOKEN_FAILURE,
        TOKEN_EXPIRED,
        ACCOUNT_LOCKED,
        PASSWORD_CHANGED,
        PROFILE_UPDATED
    }

    /**
     * Result types
     */
    public enum Result {
        SUCCESS,
        FAILURE,
        ERROR
    }
}
