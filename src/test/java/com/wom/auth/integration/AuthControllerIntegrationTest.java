package com.wom.auth.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wom.auth.config.JpaAuditingConfig;
import com.wom.auth.controller.AuthController;
import com.wom.auth.dto.LoginRequest;
import com.wom.auth.dto.LoginResponse;
import com.wom.auth.dto.RefreshTokenRequest;
import com.wom.auth.entity.User;
import com.wom.auth.exception.AccountLockedException;
import com.wom.auth.exception.InvalidCredentialsException;
import com.wom.auth.exception.InvalidTokenException;
import com.wom.auth.filter.JwtAuthenticationFilter;
import com.wom.auth.exception.GlobalExceptionHandler;
import com.wom.auth.service.AuthService;
import com.wom.auth.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for {@link AuthController}.
 * 
 * Tests HTTP layer functionality including:
 * - Request/response mapping
 * - Input validation
 * - HTTP status codes
 * - Error handling
 * 
 * Uses @WebMvcTest for lightweight controller testing without loading
 * full application context. Security filters are disabled and excluded
 * from component scanning to focus on controller logic only.
 * JPA auto-configuration is disabled since we're only testing the web layer.
 */
@WebMvcTest(
    controllers = AuthController.class,
    excludeFilters = {
        @ComponentScan.Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = {JwtAuthenticationFilter.class, JpaAuditingConfig.class}
        )
    }
)
@AutoConfigureMockMvc(addFilters = false)
@EnableAutoConfiguration(exclude = {
    DataSourceAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class,
    EnableJpaRepositories.class
})
@TestPropertySource(properties = {
    "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration"
})
@Import(GlobalExceptionHandler.class)
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @MockBean
    private UserService userService;

    // ============================================================
    // POST /auth/login - Login endpoint tests
    // ============================================================

    @Test
    void login_WithValidCredentials_ShouldReturn200AndTokens() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("ValidPass123!");

        LoginResponse response = new LoginResponse();
        response.setAccessToken("access-token-123");
        response.setRefreshToken("refresh-token-456");
        response.setExpiresIn(900000L);

        when(authService.authenticate(anyString(), anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token-123"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token-456"))
                .andExpect(jsonPath("$.expiresIn").value(900000L));
    }

    @Test
    void login_WithInvalidCredentials_ShouldReturn401() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setIdentifier("wronguser");
        request.setPassword("WrongPass123!");

        when(authService.authenticate(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException("Invalid credentials"));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid credentials"));
    }

    @Test
    void login_WithLockedAccount_ShouldReturn403() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setIdentifier("lockeduser");
        request.setPassword("ValidPass123!");

        LocalDateTime lockedUntil = LocalDateTime.now().plusHours(1);
        when(authService.authenticate(anyString(), anyString()))
                .thenThrow(new AccountLockedException("Account is locked", lockedUntil));

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Account is locked"));
    }

    @Test
    void login_WithMissingIdentifier_ShouldReturn400() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setPassword("ValidPass123!");
        // identifier is null

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithMissingPassword_ShouldReturn400() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        // password is null

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithShortPassword_ShouldReturn400() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setIdentifier("testuser");
        request.setPassword("123"); // Less than 8 characters

        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_WithEmptyRequestBody_ShouldReturn400() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // POST /auth/refresh - Token refresh endpoint tests
    // ============================================================

    @Test
    void refresh_WithValidToken_ShouldReturn200AndNewTokens() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("valid-refresh-token");

        LoginResponse response = new LoginResponse();
        response.setAccessToken("new-access-token");
        response.setRefreshToken("new-refresh-token");
        response.setExpiresIn(900000L);

        when(authService.refreshAccessToken(anyString())).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.refreshToken").value("new-refresh-token"));
    }

    @Test
    void refresh_WithInvalidToken_ShouldReturn401() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("invalid-token");

        when(authService.refreshAccessToken(anyString()))
                .thenThrow(new InvalidTokenException("Invalid refresh token"));

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid refresh token"));
    }

    @Test
    void refresh_WithMissingToken_ShouldReturn400() throws Exception {
        // Arrange
        RefreshTokenRequest request = new RefreshTokenRequest();
        // refreshToken is null

        // Act & Assert
        mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ============================================================
    // POST /auth/logout - Logout endpoint tests
    // ============================================================

    @Test
    void logout_WithValidToken_ShouldReturn200() throws Exception {
        // Arrange
        String authHeader = "Bearer valid-access-token";

        // Act & Assert
        mockMvc.perform(post("/auth/logout")
                .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));
    }

    @Test
    void logout_WithoutAuthHeader_ShouldReturn200() throws Exception {
        // Note: Without security filters, missing header is handled by controller logic
        // In real scenario with security, this would be 401
        
        // Act & Assert
        mockMvc.perform(post("/auth/logout"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // POST /auth/logout-all - Logout all devices tests
    // ============================================================

    @Test
    void logoutAll_WithValidToken_ShouldReturn200() throws Exception {
        // Arrange
        String authHeader = "Bearer valid-access-token";

        // Act & Assert
        mockMvc.perform(post("/auth/logout-all")
                .header("Authorization", authHeader))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out from all devices"));
    }

    @Test
    void logoutAll_WithoutAuthHeader_ShouldReturn200() throws Exception {
        // Act & Assert
        mockMvc.perform(post("/auth/logout-all"))
                .andExpect(status().isOk());
    }

    // ============================================================
    // GET /auth/me - Current user endpoint tests
    // ============================================================

    @Test
    @WithMockUser(username = "testuser")
    void getCurrentUser_WithValidUser_ShouldReturn200AndUserData() throws Exception {
        // Arrange
        User user = new User();
        user.setId(1L);
        user.setUsername("testuser");
        user.setEmail("test@example.com");
        user.setCreatedAt(LocalDateTime.now());

        when(userService.findByUsername("testuser")).thenReturn(Optional.of(user));

        // Act & Assert
        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));
    }

    @Test
    @WithMockUser(username = "nonexistent")
    void getCurrentUser_WithNonExistentUser_ShouldReturn404() throws Exception {
        // Arrange
        when(userService.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act & Assert
        mockMvc.perform(get("/auth/me")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User not found"));
    }
}
