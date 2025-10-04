package com.wom.auth.dto;

import com.wom.auth.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "User profile information")
public class UserResponse {
    
    @Schema(description = "User unique identifier", example = "1")
    private Long id;
    
    @Schema(description = "User email address", example = "admin@test.com")
    private String email;
    
    @Schema(description = "Username", example = "admin")
    private String username;
    
    @Schema(description = "User full name", example = "John Doe")
    private String fullName;
    
    @Schema(description = "Account status", example = "ACTIVE", allowableValues = {"ACTIVE", "LOCKED", "DISABLED"})
    private String status;
    
    @Schema(description = "Last login timestamp", example = "2025-10-03T17:13:02.825307")
    private LocalDateTime lastLoginAt;
    
    @Schema(description = "Account creation timestamp", example = "2025-10-03T16:02:52.061257")
    private LocalDateTime createdAt;

    public static UserResponse fromEntity(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .status(user.getStatus().name())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
