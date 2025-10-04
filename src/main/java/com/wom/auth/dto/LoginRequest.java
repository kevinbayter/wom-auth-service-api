package com.wom.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request with user credentials")
public class LoginRequest {

    @Schema(
            description = "User identifier - can be email or username",
            example = "admin@test.com",
            required = true
    )
    @NotBlank(message = "Identifier is required")
    private String identifier;

    @Schema(
            description = "User password",
            example = "password123",
            required = true,
            minLength = 8
    )
    @NotBlank(message = "Password is required")
    @Size(min = 8, message = "Password must be at least 8 characters")
    private String password;
}
