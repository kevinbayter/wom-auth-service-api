package com.wom.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for JpaAuditingConfig.
 */
class JpaAuditingConfigTest {

    @Test
    void jpaAuditingConfig_ShouldBeAnnotatedWithConfiguration() {
        // When
        boolean hasConfigurationAnnotation = JpaAuditingConfig.class
                .isAnnotationPresent(org.springframework.context.annotation.Configuration.class);

        // Then
        assertTrue(hasConfigurationAnnotation, "JpaAuditingConfig should be annotated with @Configuration");
    }

    @Test
    void jpaAuditingConfig_ShouldBeAnnotatedWithEnableJpaAuditing() {
        // When
        boolean hasEnableJpaAuditingAnnotation = JpaAuditingConfig.class
                .isAnnotationPresent(EnableJpaAuditing.class);

        // Then
        assertTrue(hasEnableJpaAuditingAnnotation, "JpaAuditingConfig should be annotated with @EnableJpaAuditing");
    }

    @Test
    void jpaAuditingConfig_ShouldBeInstantiable() {
        // When & Then
        assertDoesNotThrow(() -> new JpaAuditingConfig(), "JpaAuditingConfig should be instantiable");
    }

    @Test
    void jpaAuditingConfig_ShouldHavePublicConstructor() {
        // When
        JpaAuditingConfig config = new JpaAuditingConfig();

        // Then
        assertNotNull(config, "JpaAuditingConfig instance should not be null");
    }

    @Test
    void jpaAuditingConfig_MultipleInstances_ShouldBeIndependent() {
        // When
        JpaAuditingConfig config1 = new JpaAuditingConfig();
        JpaAuditingConfig config2 = new JpaAuditingConfig();

        // Then
        assertNotEquals(config1, config2, "Each instance should be independent");
    }
}
