package com.wom.auth.service;

import com.wom.auth.entity.User;
import com.wom.auth.repository.jpa.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for user management and password validation.
 */
@Slf4j
@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    /**
     * Finds user by email or username.
     *
     * @param identifier email or username
     * @return user if found
     */
    public Optional<User> findByEmailOrUsername(String identifier) {
        return userRepository.findByEmailOrUsername(identifier);
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    /**
     * Validates password using BCrypt constant-time comparison.
     *
     * @param rawPassword plain text password
     * @param encodedPassword BCrypt hash
     * @return true if password matches
     */
    public boolean validatePassword(String rawPassword, String encodedPassword) {
        boolean matches = passwordEncoder.matches(rawPassword, encodedPassword);
        log.debug("Password validation - Raw: [{}], Encoded: [{}], Matches: {}", 
                  rawPassword, encodedPassword, matches);
        return matches;
    }

    /**
     * Increments failed login attempts.
     * Locks account for 30 minutes after 5 failed attempts.
     *
     * @param user user who failed authentication
     */
    @Transactional
    public void incrementFailedAttempts(User user) {
        user.incrementFailedAttempts();
        
        if (user.getFailedAttempts() >= 5) {
            user.lockAccount(30);
        }
        
        userRepository.save(user);
    }

    @Transactional
    public void resetFailedAttempts(User user) {
        user.resetFailedAttempts();
        userRepository.save(user);
    }

    @Transactional
    public void updateLastLogin(User user) {
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public User createUser(String email, String username, String password, String fullName) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }
        
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }

        User user = User.builder()
                .email(email)
                .username(username)
                .passwordHash(passwordEncoder.encode(password))
                .fullName(fullName)
                .build();

        return userRepository.save(user);
    }

    public boolean isAccountLocked(User user) {
        return user.isLocked();
    }

    public boolean isAccountActive(User user) {
        return user.isActive();
    }
}
