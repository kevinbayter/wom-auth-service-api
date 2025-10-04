package com.wom.auth.service;

import com.wom.auth.entity.User;
import com.wom.auth.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CustomUserDetailsService}.
 * 
 * Tests Spring Security UserDetailsService implementation including:
 * - User lookup by username
 * - User lookup by email
 * - UserDetails conversion
 * - Account status mapping (active, locked, etc.)
 * - Exception handling for non-existent users
 */
@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedPassword")
                .fullName("Test User")
                .status(User.UserStatus.ACTIVE)
                .build();
    }

    @Test
    void loadUserByUsername_WithValidUsername_ShouldReturnUserDetails() {
        // Arrange
        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("$2a$10$hashedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonExpired());
        assertTrue(userDetails.isCredentialsNonExpired());
        assertTrue(userDetails.isAccountNonLocked());
        assertTrue(userDetails.getAuthorities().isEmpty());
        verify(userRepository).findByEmailOrUsername("testuser");
    }

    @Test
    void loadUserByUsername_WithValidEmail_ShouldReturnUserDetails() {
        // Arrange
        when(userRepository.findByEmailOrUsername("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("test@example.com");

        // Assert
        assertNotNull(userDetails);
        assertEquals("testuser", userDetails.getUsername());
        assertEquals("$2a$10$hashedPassword", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        verify(userRepository).findByEmailOrUsername("test@example.com");
    }

    @Test
    void loadUserByUsername_WithNonExistentUser_ShouldThrowException() {
        // Arrange
        String nonExistentUser = "nonexistent";
        when(userRepository.findByEmailOrUsername(nonExistentUser)).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(
            UsernameNotFoundException.class,
            () -> userDetailsService.loadUserByUsername(nonExistentUser)
        );

        assertEquals("User not found: " + nonExistentUser, exception.getMessage());
        verify(userRepository).findByEmailOrUsername(nonExistentUser);
    }

    @Test
    void loadUserByUsername_WithInactiveUser_ShouldReturnDisabledUserDetails() {
        // Arrange
        testUser.setStatus(User.UserStatus.INACTIVE);
        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());
        verify(userRepository).findByEmailOrUsername("testuser");
    }

    @Test
    void loadUserByUsername_WithLockedUser_ShouldReturnLockedUserDetails() {
        // Arrange
        testUser.setLockedUntil(LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        // isEnabled() is false because isActive() returns false when locked
        assertFalse(userDetails.isEnabled());
        assertFalse(userDetails.isAccountNonLocked());
        verify(userRepository).findByEmailOrUsername("testuser");
    }

    @Test
    void loadUserByUsername_WithExpiredLock_ShouldReturnUnlockedUserDetails() {
        // Arrange
        testUser.setLockedUntil(LocalDateTime.now().minusHours(1));
        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());
        verify(userRepository).findByEmailOrUsername("testuser");
    }

    @Test
    void loadUserByUsername_WithInactiveAndLockedUser_ShouldReturnDisabledAndLockedUserDetails() {
        // Arrange
        testUser.setStatus(User.UserStatus.INACTIVE);
        testUser.setLockedUntil(LocalDateTime.now().plusHours(1));
        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
        assertFalse(userDetails.isAccountNonLocked());
        verify(userRepository).findByEmailOrUsername("testuser");
    }

    @Test
    void loadUserByUsername_ShouldAlwaysReturnEmptyAuthorities() {
        // Arrange
        when(userRepository.findByEmailOrUsername(anyString())).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertNotNull(userDetails.getAuthorities());
        assertTrue(userDetails.getAuthorities().isEmpty());
    }

    @Test
    void loadUserByUsername_ShouldAlwaysSetAccountNonExpiredToTrue() {
        // Arrange
        when(userRepository.findByEmailOrUsername(anyString())).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertTrue(userDetails.isAccountNonExpired());
    }

    @Test
    void loadUserByUsername_ShouldAlwaysSetCredentialsNonExpiredToTrue() {
        // Arrange
        when(userRepository.findByEmailOrUsername(anyString())).thenReturn(Optional.of(testUser));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("testuser");

        // Assert
        assertTrue(userDetails.isCredentialsNonExpired());
    }
}
