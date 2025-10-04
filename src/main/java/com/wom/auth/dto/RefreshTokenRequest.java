package com.wom.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to refresh access token")
public class RefreshTokenRequest {

    @Schema(
            description = "Valid JWT refresh token obtained from login or previous refresh",
            example = "eyJhbGciOiJSUzI1NiJ9...",
            required = true
    )
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
