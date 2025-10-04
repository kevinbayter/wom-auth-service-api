package com.wom.auth.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Service for managing custom application metrics.
 * 
 * Provides methods to record authentication-related metrics such as:
 * - Login success/failure counters
 * - Authentication operation latencies
 * - Refresh token operation metrics
 * 
 * These metrics are exposed via Actuator's /actuator/prometheus endpoint
 * and can be scraped by Prometheus for monitoring and alerting.
 */
@Service
public class MetricsService {

    private static final String METRIC_PREFIX = "auth";
    
    private final Counter loginSuccessCounter;
    private final Counter loginFailureCounter;
    private final Counter refreshSuccessCounter;
    private final Counter refreshFailureCounter;
    private final Counter logoutCounter;
    
    private final Timer loginTimer;
    private final Timer refreshTimer;
    
    private final MeterRegistry meterRegistry;

    public MetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Initialize counters
        this.loginSuccessCounter = Counter.builder(METRIC_PREFIX + ".login.success")
                .description("Total number of successful login attempts")
                .tag("operation", "login")
                .tag("result", "success")
                .register(meterRegistry);
        
        this.loginFailureCounter = Counter.builder(METRIC_PREFIX + ".login.failure")
                .description("Total number of failed login attempts")
                .tag("operation", "login")
                .tag("result", "failure")
                .register(meterRegistry);
        
        this.refreshSuccessCounter = Counter.builder(METRIC_PREFIX + ".refresh.success")
                .description("Total number of successful token refresh operations")
                .tag("operation", "refresh")
                .tag("result", "success")
                .register(meterRegistry);
        
        this.refreshFailureCounter = Counter.builder(METRIC_PREFIX + ".refresh.failure")
                .description("Total number of failed token refresh operations")
                .tag("operation", "refresh")
                .tag("result", "failure")
                .register(meterRegistry);
        
        this.logoutCounter = Counter.builder(METRIC_PREFIX + ".logout")
                .description("Total number of logout operations")
                .tag("operation", "logout")
                .register(meterRegistry);
        
        // Initialize timers for latency tracking
        this.loginTimer = Timer.builder(METRIC_PREFIX + ".login.latency")
                .description("Latency of login operations")
                .tag("operation", "login")
                .register(meterRegistry);
        
        this.refreshTimer = Timer.builder(METRIC_PREFIX + ".refresh.latency")
                .description("Latency of refresh token operations")
                .tag("operation", "refresh")
                .register(meterRegistry);
    }

    /**
     * Records a successful login attempt.
     */
    public void recordLoginSuccess() {
        loginSuccessCounter.increment();
    }

    /**
     * Records a failed login attempt.
     */
    public void recordLoginFailure() {
        loginFailureCounter.increment();
    }

    /**
     * Records a successful token refresh.
     */
    public void recordRefreshSuccess() {
        refreshSuccessCounter.increment();
    }

    /**
     * Records a failed token refresh.
     */
    public void recordRefreshFailure() {
        refreshFailureCounter.increment();
    }

    /**
     * Records a logout operation.
     */
    public void recordLogout() {
        logoutCounter.increment();
    }

    /**
     * Records the latency of a login operation.
     * 
     * @param durationMs duration in milliseconds
     */
    public void recordLoginLatency(long durationMs) {
        loginTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Records the latency of a refresh operation.
     * 
     * @param durationMs duration in milliseconds
     */
    public void recordRefreshLatency(long durationMs) {
        refreshTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Executes a login operation and records its latency.
     * 
     * @param <T> return type of the operation
     * @param operation the operation to execute
     * @return the result of the operation
     */
    public <T> T recordLoginOperation(LoginOperation<T> operation) {
        return loginTimer.record(operation::execute);
    }

    /**
     * Executes a refresh operation and records its latency.
     * 
     * @param <T> return type of the operation
     * @param operation the operation to execute
     * @return the result of the operation
     */
    public <T> T recordRefreshOperation(RefreshOperation<T> operation) {
        return refreshTimer.record(operation::execute);
    }

    @FunctionalInterface
    public interface LoginOperation<T> {
        T execute();
    }

    @FunctionalInterface
    public interface RefreshOperation<T> {
        T execute();
    }
}
