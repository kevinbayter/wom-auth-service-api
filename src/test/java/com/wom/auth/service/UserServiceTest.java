package com.wom.auth.service;

import com.wom.auth.entity.User;
import com.wom.auth.repository.jpa.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link UserService}.
 */
@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;
    private BCryptPasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder();
        ReflectionTestUtils.setField(userService, "passwordEncoder", passwordEncoder);
        
        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .username("testuser")
                .passwordHash(passwordEncoder.encode("password123"))
                .status(User.UserStatus.ACTIVE)
                .failedAttempts(0)
                .build();
    }

    @Test
    void findByEmail_WithExistingEmail_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByEmail("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository, times(1)).findByEmail("test@example.com");
    }

    @Test
    void findByEmail_WithNonExistingEmail_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByEmail("nonexistent@example.com");

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByEmail("nonexistent@example.com");
    }

    @Test
    void findByUsername_WithExistingUsername_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository, times(1)).findByUsername("testuser");
    }

    @Test
    void findByUsername_WithNonExistingUsername_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findByUsername("nonexistent");

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findByUsername("nonexistent");
    }

    @Test
    void findByEmailOrUsername_WithEmail_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByEmailOrUsername("test@example.com")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByEmailOrUsername("test@example.com");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("test@example.com", result.get().getEmail());
        verify(userRepository, times(1)).findByEmailOrUsername("test@example.com");
    }

    @Test
    void findByEmailOrUsername_WithUsername_ShouldReturnUser() {
        // Arrange
        when(userRepository.findByEmailOrUsername("testuser")).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findByEmailOrUsername("testuser");

        // Assert
        assertTrue(result.isPresent());
        assertEquals("testuser", result.get().getUsername());
        verify(userRepository, times(1)).findByEmailOrUsername("testuser");
    }

    @Test
    void findById_WithExistingId_ShouldReturnUser() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act
        Optional<User> result = userService.findById(1L);

        // Assert
        assertTrue(result.isPresent());
        assertEquals(1L, result.get().getId());
        verify(userRepository, times(1)).findById(1L);
    }

    @Test
    void findById_WithNonExistingId_ShouldReturnEmpty() {
        // Arrange
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act
        Optional<User> result = userService.findById(999L);

        // Assert
        assertFalse(result.isPresent());
        verify(userRepository, times(1)).findById(999L);
    }

    @Test
    void validatePassword_WithCorrectPassword_ShouldReturnTrue() {
        // Arrange
        String rawPassword = "password123";
        String encodedPassword = passwordEncoder.encode(rawPassword);

        // Act
        boolean result = userService.validatePassword(rawPassword, encodedPassword);

        // Assert
        assertTrue(result);
    }

    @Test
    void validatePassword_WithIncorrectPassword_ShouldReturnFalse() {
        // Arrange
        String rawPassword = "wrongpassword";
        String encodedPassword = passwordEncoder.encode("password123");

        // Act
        boolean result = userService.validatePassword(rawPassword, encodedPassword);

        // Assert
        assertFalse(result);
    }

    @Test
    void isAccountLocked_WithLockedAccount_ShouldReturnTrue() {
        // Arrange
        testUser.setLockedUntil(LocalDateTime.now().plusMinutes(30));

        // Act
        boolean result = userService.isAccountLocked(testUser);

        // Assert
        assertTrue(result);
    }

    @Test
    void isAccountLocked_WithUnlockedAccount_ShouldReturnFalse() {
        // Arrange
        testUser.setLockedUntil(null);

        // Act
        boolean result = userService.isAccountLocked(testUser);

        // Assert
        assertFalse(result);
    }

    @Test
    void isAccountLocked_WithExpiredLock_ShouldReturnFalse() {
        // Arrange
        testUser.setLockedUntil(LocalDateTime.now().minusMinutes(1));

        // Act
        boolean result = userService.isAccountLocked(testUser);

        // Assert
        assertFalse(result);
    }

    @Test
    void isAccountActive_WithActiveAccount_ShouldReturnTrue() {
        // Arrange
        testUser.setStatus(User.UserStatus.ACTIVE);

        // Act
        boolean result = userService.isAccountActive(testUser);

        // Assert
        assertTrue(result);
    }

    @Test
    void isAccountActive_WithInactiveAccount_ShouldReturnFalse() {
        // Arrange
        testUser.setStatus(User.UserStatus.INACTIVE);

        // Act
        boolean result = userService.isAccountActive(testUser);

        // Assert
        assertFalse(result);
    }

    @Test
    void incrementFailedAttempts_ShouldIncreaseCountAndSave() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.incrementFailedAttempts(testUser);

        // Assert
        assertEquals(1, testUser.getFailedAttempts());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void resetFailedAttempts_ShouldSetCountToZeroAndSave() {
        // Arrange
        testUser.incrementFailedAttempts();
        testUser.incrementFailedAttempts();
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.resetFailedAttempts(testUser);

        // Assert
        assertEquals(0, testUser.getFailedAttempts());
        assertNull(testUser.getLockedUntil());
        verify(userRepository, times(1)).save(testUser);
    }

    @Test
    void updateLastLogin_ShouldSetLastLoginTimeAndSave() {
        // Arrange
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // Act
        userService.updateLastLogin(testUser);

        // Assert
        assertNotNull(testUser.getLastLoginAt());
        assertTrue(testUser.getLastLoginAt().isBefore(LocalDateTime.now().plusSeconds(1)));
        verify(userRepository, times(1)).save(testUser);
    }
}
