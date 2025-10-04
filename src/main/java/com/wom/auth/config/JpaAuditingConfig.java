package com.wom.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Configuration class for JPA Auditing.
 * Separated from main application class to allow conditional loading in tests.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
