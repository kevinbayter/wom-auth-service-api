package com.wom.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Authentication response with JWT tokens")
public class LoginResponse {
    
    @Schema(
            description = "JWT access token for API authentication",
            example = "eyJhbGciOiJSUzI1NiJ9...",
            required = true
    )
    private String accessToken;
    
    @Schema(
            description = "JWT refresh token for obtaining new access tokens",
            example = "eyJhbGciOiJSUzI1NiJ9...",
            required = true
    )
    private String refreshToken;
    
    @Schema(
            description = "Token type - always Bearer",
            example = "Bearer",
            required = true
    )
    private String tokenType;
    
    @Schema(
            description = "Access token expiration time in seconds",
            example = "900",
            required = true
    )
    private Long expiresIn;
}
