package com.wom.auth.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidCredentials(
            InvalidCredentialsException ex, WebRequest request) {
        log.warn("Invalid credentials attempt: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(TokenExpiredException.class)
    public ResponseEntity<Map<String, Object>> handleTokenExpired(
            TokenExpiredException ex, WebRequest request) {
        log.debug("Token expired: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidToken(
            InvalidTokenException ex, WebRequest request) {
        log.warn("Invalid token: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLocked(
            AccountLockedException ex, WebRequest request) {
        log.warn("Account locked: {}", ex.getMessage());
        Map<String, Object> response = buildErrorMap(HttpStatus.FORBIDDEN, ex.getMessage(), request);
        response.put("lockedUntil", ex.getLockedUntil());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<Map<String, Object>> handleRateLimitExceeded(
            RateLimitExceededException ex, WebRequest request) {
        log.warn("Rate limit exceeded: {}", ex.getMessage());
        return buildErrorResponse(HttpStatus.TOO_MANY_REQUESTS, ex.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex, WebRequest request) {
        log.error("Unhandled exception", ex);
        return buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred",
                request
        );
    }

    private ResponseEntity<Map<String, Object>> buildErrorResponse(
            HttpStatus status, String message, WebRequest request) {
        return ResponseEntity
                .status(status)
                .body(buildErrorMap(status, message, request));
    }

    private Map<String, Object> buildErrorMap(
            HttpStatus status, String message, WebRequest request) {
        Map<String, Object> error = new HashMap<>();
        error.put("timestamp", LocalDateTime.now());
        error.put("status", status.value());
        error.put("error", status.getReasonPhrase());
        error.put("message", message);
        error.put("path", request.getDescription(false).replace("uri=", ""));
        return error;
    }
}
