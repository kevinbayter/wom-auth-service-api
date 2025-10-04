package com.wom.auth.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for {@link MetricsService}.
 */
class MetricsServiceTest {

    private MeterRegistry meterRegistry;
    private MetricsService metricsService;

    @BeforeEach
    void setUp() {
        // Use SimpleMeterRegistry for testing
        meterRegistry = new SimpleMeterRegistry();
        metricsService = new MetricsService(meterRegistry);
    }

    @Test
    void recordLoginSuccess_ShouldIncrementCounter() {
        // Arrange
        double beforeCount = getCountValue("auth.login.success");

        // Act
        metricsService.recordLoginSuccess();

        // Assert
        double afterCount = getCountValue("auth.login.success");
        assertEquals(beforeCount + 1, afterCount, "Login success counter should increment");
    }

    @Test
    void recordLoginFailure_ShouldIncrementCounter() {
        // Arrange
        double beforeCount = getCountValue("auth.login.failure");

        // Act
        metricsService.recordLoginFailure();

        // Assert
        double afterCount = getCountValue("auth.login.failure");
        assertEquals(beforeCount + 1, afterCount, "Login failure counter should increment");
    }

    @Test
    void recordRefreshSuccess_ShouldIncrementCounter() {
        // Arrange
        double beforeCount = getCountValue("auth.refresh.success");

        // Act
        metricsService.recordRefreshSuccess();

        // Assert
        double afterCount = getCountValue("auth.refresh.success");
        assertEquals(beforeCount + 1, afterCount, "Refresh success counter should increment");
    }

    @Test
    void recordRefreshFailure_ShouldIncrementCounter() {
        // Arrange
        double beforeCount = getCountValue("auth.refresh.failure");

        // Act
        metricsService.recordRefreshFailure();

        // Assert
        double afterCount = getCountValue("auth.refresh.failure");
        assertEquals(beforeCount + 1, afterCount, "Refresh failure counter should increment");
    }

    @Test
    void recordLogout_ShouldIncrementCounter() {
        // Arrange
        double beforeCount = getCountValue("auth.logout");

        // Act
        metricsService.recordLogout();

        // Assert
        double afterCount = getCountValue("auth.logout");
        assertEquals(beforeCount + 1, afterCount, "Logout counter should increment");
    }

    @Test
    void recordLoginLatency_ShouldRecordTimerValue() {
        // Arrange
        long latencyMs = 150L;
        Timer timer = meterRegistry.find("auth.login.latency").timer();
        long beforeCount = timer != null ? timer.count() : 0;

        // Act
        metricsService.recordLoginLatency(latencyMs);

        // Assert
        timer = meterRegistry.find("auth.login.latency").timer();
        assertNotNull(timer);
        assertEquals(beforeCount + 1, timer.count());
    }

    @Test
    void recordRefreshLatency_ShouldRecordTimerValue() {
        // Arrange
        long latencyMs = 75L;
        Timer timer = meterRegistry.find("auth.refresh.latency").timer();
        long beforeCount = timer != null ? timer.count() : 0;

        // Act
        metricsService.recordRefreshLatency(latencyMs);

        // Assert
        timer = meterRegistry.find("auth.refresh.latency").timer();
        assertNotNull(timer);
        assertEquals(beforeCount + 1, timer.count());
    }

    @Test
    void recordLoginOperation_WithSuccessfulExecution_ShouldReturnResult() {
        // Arrange
        MetricsService.LoginOperation<String> operation = () -> "success";

        // Act
        String result = metricsService.recordLoginOperation(operation);

        // Assert
        assertEquals("success", result);
    }

    @Test
    void recordLoginOperation_WithException_ShouldRethrowException() {
        // Arrange
        RuntimeException expectedException = new RuntimeException("Login failed");
        MetricsService.LoginOperation<String> operation = () -> {
            throw expectedException;
        };

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            metricsService.recordLoginOperation(operation);
        });

        assertEquals(expectedException, thrown);
    }

    @Test
    void recordRefreshOperation_WithSuccessfulExecution_ShouldReturnResult() {
        // Arrange
        MetricsService.RefreshOperation<String> operation = () -> "refreshed";

        // Act
        String result = metricsService.recordRefreshOperation(operation);

        // Assert
        assertEquals("refreshed", result);
    }

    @Test
    void recordRefreshOperation_WithException_ShouldRethrowException() {
        // Arrange
        RuntimeException expectedException = new RuntimeException("Refresh failed");
        MetricsService.RefreshOperation<String> operation = () -> {
            throw expectedException;
        };

        // Act & Assert
        RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
            metricsService.recordRefreshOperation(operation);
        });

        assertEquals(expectedException, thrown);
    }

    @Test
    void recordLoginOperation_ShouldWorkWithVoidOperations() {
        // Arrange
        final boolean[] executed = {false};
        MetricsService.LoginOperation<Void> operation = () -> {
            executed[0] = true;
            return null;
        };

        // Act
        metricsService.recordLoginOperation(operation);

        // Assert
        assertTrue(executed[0], "Operation should have been executed");
    }

    @Test
    void recordRefreshOperation_ShouldWorkWithVoidOperations() {
        // Arrange
        final boolean[] executed = {false};
        MetricsService.RefreshOperation<Void> operation = () -> {
            executed[0] = true;
            return null;
        };

        // Act
        metricsService.recordRefreshOperation(operation);

        // Assert
        assertTrue(executed[0], "Operation should have been executed");
    }

    @Test
    void multipleLoginSuccesses_ShouldIncrementCounterMultipleTimes() {
        // Arrange
        double beforeCount = meterRegistry.counter("auth.login.success", "operation", "login", "result", "success").count();

        // Act
        metricsService.recordLoginSuccess();
        metricsService.recordLoginSuccess();
        metricsService.recordLoginSuccess();

        // Assert
        double afterCount = meterRegistry.counter("auth.login.success", "operation", "login", "result", "success").count();
        assertEquals(beforeCount + 3, afterCount);
    }

    @Test
    void mixedLoginOperations_ShouldRecordBothSuccessAndFailure() {
        // Arrange
        double beforeSuccessCount = getCountValue("auth.login.success");
        double beforeFailureCount = getCountValue("auth.login.failure");
        
        MetricsService.LoginOperation<String> successOp = () -> "success";
        MetricsService.LoginOperation<String> failureOp = () -> {
            throw new RuntimeException("fail");
        };

        // Act
        metricsService.recordLoginOperation(successOp);
        
        assertThrows(RuntimeException.class, () -> {
            metricsService.recordLoginOperation(failureOp);
        });

        // Assert
        double afterSuccessCount = getCountValue("auth.login.success");
        double afterFailureCount = getCountValue("auth.login.failure");
        
        assertEquals(beforeSuccessCount + 1, afterSuccessCount, "Should record one success");
        assertEquals(beforeFailureCount + 1, afterFailureCount, "Should record one failure");
    }

    @Test
    void recordLoginLatency_WithMultipleRecordings_ShouldTrackAll() {
        // Arrange
        Timer timer = meterRegistry.find("auth.login.latency").timer();
        long beforeCount = timer != null ? timer.count() : 0;

        // Act
        metricsService.recordLoginLatency(100L);
        metricsService.recordLoginLatency(200L);
        metricsService.recordLoginLatency(150L);

        // Assert
        timer = meterRegistry.find("auth.login.latency").timer();
        assertNotNull(timer);
        assertEquals(beforeCount + 3, timer.count());
    }

    /**
     * Helper method to get counter value from the registry.
     * This searches for the counter by name in the SimpleMeterRegistry.
     */
    private double getCountValue(String counterName) {
        return meterRegistry.find(counterName)
                .counters()
                .stream()
                .mapToDouble(counter -> counter.count())
                .sum();
    }
}

