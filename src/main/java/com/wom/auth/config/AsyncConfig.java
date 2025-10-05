package com.wom.auth.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Configuration for asynchronous processing.
 * Enables @Async annotation for non-blocking operations.
 */
@Configuration
@EnableAsync
public class AsyncConfig {
    // Async processing enabled for audit logging
    // Uses default task executor from Spring Boot
}
