package com.wom.auth.filter;

import com.wom.auth.exception.InvalidTokenException;
import com.wom.auth.exception.TokenExpiredException;
import com.wom.auth.service.JwtService;
import com.wom.auth.service.TokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link JwtAuthenticationFilter}.
 * 
 * Tests JWT token validation and authentication flow including:
 * - Valid token authentication
 * - Missing/invalid Authorization header
 * - Blacklisted tokens
 * - Expired tokens
 * - Invalid tokens
 * - Error handling
 */
@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

    @Mock
    private JwtService jwtService;

    @Mock
    private TokenService tokenService;

    @Mock
    private UserDetailsService userDetailsService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private JwtAuthenticationFilter filter;

    @BeforeEach
    void setUp() {
        filter = new JwtAuthenticationFilter(jwtService, tokenService, userDetailsService);
        SecurityContextHolder.clearContext();
    }

    @Test
    void doFilterInternal_WithNoAuthHeader_ShouldContinueFilterChain() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithInvalidAuthHeaderFormat_ShouldContinueFilterChain() throws Exception {
        // Arrange
        when(request.getHeader("Authorization")).thenReturn("InvalidFormat token123");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(jwtService, never()).validateToken(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithValidToken_ShouldAuthenticateUser() throws Exception {
        // Arrange
        String token = "valid.jwt.token";
        String username = "testuser";
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtService.validateToken(token)).thenReturn(null); // Returns Claims, but we don't use it
        when(jwtService.isTokenExpired(token)).thenReturn(false);
        when(jwtService.getUsernameFromToken(token)).thenReturn(username);
        when(userDetailsService.loadUserByUsername(username)).thenReturn(userDetails);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(jwtService).validateToken(token);
        verify(userDetailsService).loadUserByUsername(username);
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(username, SecurityContextHolder.getContext().getAuthentication().getName());
    }

    @Test
    void doFilterInternal_WithBlacklistedToken_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        String token = "blacklisted.jwt.token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.isTokenBlacklisted(token)).thenReturn(true);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(writer);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("Token has been revoked"));
    }

    @Test
    void doFilterInternal_WithExpiredToken_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        String token = "expired.jwt.token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtService.validateToken(token)).thenReturn(null);
        when(jwtService.isTokenExpired(token)).thenReturn(true);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(writer);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("Token has expired"));
    }

    @Test
    void doFilterInternal_WithTokenExpiredException_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        String token = "expired.jwt.token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.isTokenBlacklisted(token)).thenReturn(false);
        doThrow(new TokenExpiredException("Token has expired")).when(jwtService).validateToken(token);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(writer);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("Token has expired"));
    }

    @Test
    void doFilterInternal_WithInvalidTokenException_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        String token = "invalid.jwt.token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.isTokenBlacklisted(token)).thenReturn(false);
        doThrow(new InvalidTokenException("Invalid token signature")).when(jwtService).validateToken(token);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(writer);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("Invalid token signature"));
    }

    @Test
    void doFilterInternal_WithGenericException_ShouldReturnUnauthorized() throws Exception {
        // Arrange
        String token = "malformed.jwt.token";
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.isTokenBlacklisted(token)).thenReturn(false);
        doThrow(new RuntimeException("Unexpected error")).when(jwtService).validateToken(token);
        when(request.getRequestURI()).thenReturn("/api/test");
        when(response.getWriter()).thenReturn(writer);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(401);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);
        assertTrue(stringWriter.toString().contains("Invalid token"));
    }

    @Test
    void doFilterInternal_WithNullUsername_ShouldContinueWithoutAuth() throws Exception {
        // Arrange
        String token = "valid.jwt.token";

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtService.validateToken(token)).thenReturn(null);
        when(jwtService.isTokenExpired(token)).thenReturn(false);
        when(jwtService.getUsernameFromToken(token)).thenReturn(null);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doFilterInternal_WithExistingAuthentication_ShouldNotReauthenticate() throws Exception {
        // Arrange
        String token = "valid.jwt.token";
        String username = "testuser";
        UserDetails userDetails = User.builder()
                .username(username)
                .password("password")
                .authorities(Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")))
                .build();

        // Set existing authentication
        SecurityContextHolder.getContext().setAuthentication(
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities()
                )
        );

        when(request.getHeader("Authorization")).thenReturn("Bearer " + token);
        when(tokenService.isTokenBlacklisted(token)).thenReturn(false);
        when(jwtService.validateToken(token)).thenReturn(null);
        when(jwtService.isTokenExpired(token)).thenReturn(false);
        when(jwtService.getUsernameFromToken(token)).thenReturn(username);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verify(userDetailsService, never()).loadUserByUsername(anyString());
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
