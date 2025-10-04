package com.wom.auth.health;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Custom health indicator for PostgreSQL database connectivity.
 * 
 * Performs an active health check by executing a simple query
 * to verify that the database is accessible and responsive.
 * 
 * This provides more detailed health information than the default
 * DataSourceHealthIndicator, including query execution time.
 */
@Component
public class PostgresHealthIndicator implements HealthIndicator {

    private static final String HEALTH_CHECK_QUERY = "SELECT 1";
    private static final int TIMEOUT_SECONDS = 3;
    
    private final DataSource dataSource;

    public PostgresHealthIndicator(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public Health health() {
        try {
            long startTime = System.currentTimeMillis();
            
            try (Connection connection = dataSource.getConnection()) {
                connection.setNetworkTimeout(null, TIMEOUT_SECONDS * 1000);
                
                try (Statement statement = connection.createStatement();
                     ResultSet resultSet = statement.executeQuery(HEALTH_CHECK_QUERY)) {
                    
                    if (resultSet.next()) {
                        long responseTime = System.currentTimeMillis() - startTime;
                        
                        return Health.up()
                                .withDetail("database", "PostgreSQL")
                                .withDetail("status", "UP")
                                .withDetail("responseTime", responseTime + "ms")
                                .withDetail("query", HEALTH_CHECK_QUERY)
                                .build();
                    } else {
                        return Health.down()
                                .withDetail("database", "PostgreSQL")
                                .withDetail("reason", "Query returned no results")
                                .build();
                    }
                }
            }
        } catch (SQLException e) {
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "PostgreSQL")
                    .withDetail("error", e.getClass().getSimpleName())
                    .withDetail("message", e.getMessage())
                    .build();
        }
    }
}
