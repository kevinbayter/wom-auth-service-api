package com.wom.auth.repository;

import com.wom.auth.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for AuditLog entity.
 * Provides methods to query audit logs with various filters.
 */
@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * Find audit logs by user ID.
     *
     * @param userId user ID
     * @return list of audit logs for the user
     */
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * Find audit logs by action type.
     *
     * @param action action type
     * @return list of audit logs for the action
     */
    List<AuditLog> findByActionOrderByCreatedAtDesc(String action);

    /**
     * Find audit logs by IP address.
     *
     * @param ipAddress IP address
     * @return list of audit logs from the IP
     */
    List<AuditLog> findByIpAddressOrderByCreatedAtDesc(String ipAddress);

    /**
     * Find recent failed login attempts for a user.
     *
     * @param identifier user email or username
     * @param action action type (LOGIN_FAILURE)
     * @param since time threshold
     * @return count of failed attempts
     */
    @Query("SELECT COUNT(a) FROM AuditLog a WHERE a.identifier = :identifier AND a.action = :action AND a.createdAt >= :since")
    Long countRecentFailedAttempts(
            @Param("identifier") String identifier,
            @Param("action") String action,
            @Param("since") LocalDateTime since
    );

    /**
     * Find audit logs within a date range.
     *
     * @param startDate start date
     * @param endDate end date
     * @return list of audit logs
     */
    List<AuditLog> findByCreatedAtBetweenOrderByCreatedAtDesc(
            LocalDateTime startDate,
            LocalDateTime endDate
    );
}
