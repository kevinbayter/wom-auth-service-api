package com.wom.auth.controller;

import com.wom.auth.dto.LoginRequest;
import com.wom.auth.dto.LoginResponse;
import com.wom.auth.dto.RefreshTokenRequest;
import com.wom.auth.dto.UserResponse;
import com.wom.auth.entity.User;
import com.wom.auth.service.AuthService;
import com.wom.auth.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Authentication", description = "Authentication management endpoints")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;

    @Operation(
            summary = "Authenticate user",
            description = "Authenticates a user with email/username and password. Returns JWT access token and refresh token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "403", description = "Account locked due to failed attempts"),
            @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login attempt for identifier: {}", request.getIdentifier());
        LoginResponse response = authService.authenticate(request.getIdentifier(), request.getPassword());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Refresh access token",
            description = "Refreshes an expired access token using a valid refresh token. Returns new access and refresh tokens with token rotation."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or expired refresh token"),
            @ApiResponse(responseCode = "429", description = "Too many requests - rate limit exceeded")
    })
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        log.debug("Token refresh requested");
        LoginResponse response = authService.refreshAccessToken(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "Logout user",
            description = "Logs out the current user by blacklisting the access token. Requires valid JWT token in Authorization header.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token")
    })
    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logout(token);
        log.info("User logged out successfully");
        return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
    }

    @Operation(
            summary = "Logout from all devices",
            description = "Logs out the user from all devices by revoking all refresh tokens and blacklisting current access token. Requires valid JWT token.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logged out from all devices successfully"),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token")
    })
    @PostMapping("/logout-all")
    public ResponseEntity<Map<String, String>> logoutAll(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.substring(7);
        authService.logoutAllDevices(token);
        log.info("User logged out from all devices");
        return ResponseEntity.ok(Map.of("message", "Logged out from all devices"));
    }

    @Operation(
            summary = "Get current user profile",
            description = "Returns the profile information of the currently authenticated user. Requires valid JWT token in Authorization header.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User profile retrieved successfully",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))),
            @ApiResponse(responseCode = "401", description = "Invalid or missing token"),
            @ApiResponse(responseCode = "404", description = "User not found")
    })
    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String username = auth.getName();
        
        User user = userService.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        return ResponseEntity.ok(UserResponse.fromEntity(user));
    }
}
