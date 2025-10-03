package com.wom.auth.controller;

import com.wom.auth.dto.LoginRequest;
import com.wom.auth.dto.LoginResponse;
import com.wom.auth.dto.RefreshTokenRequest;
import com.wom.auth.dto.UserResponse;
import com.wom.auth.entity.User;
import com.wom.auth.service.AuthService;
import com.wom.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for identifier: {}", request.getIdentifier());
        LoginResponse response = authService.authenticate(request.getIdentifier(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh requested");
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        log.info("User logged out successfully");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logoutAllDevices(token);
        log.info("User logged out from all devices");
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
