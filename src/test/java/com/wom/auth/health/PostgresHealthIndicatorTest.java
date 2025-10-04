package com.wom.auth.health;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for PostgresHealthIndicator.
 * Tests database health check functionality including success, failure, and timeout scenarios.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PostgresHealthIndicator Tests")
class PostgresHealthIndicatorTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private Statement statement;

    @Mock
    private ResultSet resultSet;

    private PostgresHealthIndicator healthIndicator;

    @BeforeEach
    void setUp() {
        healthIndicator = new PostgresHealthIndicator(dataSource);
    }

    @Test
    @DisplayName("Should return UP status when database is healthy")
    void shouldReturnUpWhenDatabaseIsHealthy() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        assertThat(health.getDetails())
                .containsKey("database")
                .containsKey("status")
                .containsKey("responseTime")
                .containsKey("query");
        assertThat(health.getDetails().get("database")).isEqualTo("PostgreSQL");
        assertThat(health.getDetails().get("status")).isEqualTo("UP");
        assertThat(health.getDetails().get("query")).isEqualTo("SELECT 1");

        verify(dataSource).getConnection();
        verify(connection).createStatement();
        verify(statement).executeQuery("SELECT 1");
        verify(resultSet).next();
    }

    @Test
    @DisplayName("Should return DOWN status when SQLException occurs")
    void shouldReturnDownWhenSQLExceptionOccurs() throws Exception {
        // Given
        SQLException sqlException = new SQLException("Connection refused");
        when(dataSource.getConnection()).thenThrow(sqlException);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsKey("database")
                .containsKey("error")
                .containsKey("message");
        assertThat(health.getDetails().get("database")).isEqualTo("PostgreSQL");
        assertThat(health.getDetails().get("error")).isEqualTo("SQLException");
        assertThat(health.getDetails().get("message")).asString().contains("Connection refused");

        verify(dataSource).getConnection();
    }

    @Test
    @DisplayName("Should return DOWN status when generic exception occurs")
    void shouldReturnDownWhenGenericExceptionOccurs() throws Exception {
        // Given
        RuntimeException runtimeException = new RuntimeException("Unexpected error");
        when(dataSource.getConnection()).thenThrow(runtimeException);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsKey("database")
                .containsKey("error")
                .containsKey("message");
        assertThat(health.getDetails().get("database")).isEqualTo("PostgreSQL");
        assertThat(health.getDetails().get("error")).isEqualTo("RuntimeException");
        assertThat(health.getDetails().get("message")).asString().contains("Unexpected error");

        verify(dataSource).getConnection();
    }

    @Test
    @DisplayName("Should return DOWN when query returns no results")
    void shouldReturnDownWhenQueryReturnsNoResults() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails())
                .containsKey("database")
                .containsKey("reason");
        assertThat(health.getDetails().get("database")).isEqualTo("PostgreSQL");
        assertThat(health.getDetails().get("reason")).isEqualTo("Query returned no results");
    }

    @Test
    @DisplayName("Should measure response time accurately")
    void shouldMeasureResponseTimeAccurately() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenAnswer(invocation -> {
            Thread.sleep(50); // Simulate delay
            return resultSet;
        });
        when(resultSet.next()).thenReturn(true);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.UP);
        Object responseTime = health.getDetails().get("responseTime");
        assertThat(responseTime).isNotNull();
        assertThat(responseTime.toString()).matches("\\d+ms");
        
        // Response time should be at least 50ms
        String timeStr = responseTime.toString().replace("ms", "");
        long time = Long.parseLong(timeStr);
        assertThat(time).isGreaterThanOrEqualTo(50L);
    }

    @Test
    @DisplayName("Should execute SELECT 1 query")
    void shouldExecuteSelect1Query() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        // When
        healthIndicator.health();

        // Then
        verify(statement).executeQuery("SELECT 1");
    }

    @Test
    @DisplayName("Should set network timeout on connection")
    void shouldSetNetworkTimeoutOnConnection() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        // When
        healthIndicator.health();

        // Then
        verify(connection).setNetworkTimeout(isNull(), eq(3000));
    }

    @Test
    @DisplayName("Should include all required details in UP status")
    void shouldIncludeAllRequiredDetailsInUpStatus() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getDetails()).hasSize(4); // database, status, responseTime, query
        assertThat(health.getDetails())
                .containsKey("database")
                .containsKey("status")
                .containsKey("responseTime")
                .containsKey("query");
    }

    @Test
    @DisplayName("Should include required details in DOWN status with error")
    void shouldIncludeRequiredDetailsInDownStatus() throws Exception {
        // Given
        SQLException sqlException = new SQLException("Test error");
        when(dataSource.getConnection()).thenThrow(sqlException);

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getDetails()).hasSize(3); // database, error, message
        assertThat(health.getDetails())
                .containsKey("database")
                .containsKey("error")
                .containsKey("message");
    }

    @Test
    @DisplayName("Should handle statement execution failure")
    void shouldHandleStatementExecutionFailure() throws Exception {
        // Given
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.createStatement()).thenReturn(statement);
        when(statement.executeQuery(anyString())).thenThrow(new SQLException("Query timeout"));

        // When
        Health health = healthIndicator.health();

        // Then
        assertThat(health.getStatus()).isEqualTo(Status.DOWN);
        assertThat(health.getDetails().get("error")).isEqualTo("SQLException");
        assertThat(health.getDetails().get("message")).asString().contains("Query timeout");
    }
}
