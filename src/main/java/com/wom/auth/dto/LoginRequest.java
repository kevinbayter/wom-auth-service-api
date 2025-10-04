package com.wom.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;

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
            example = "password",
            required = true
    )
    @NotBlank(message = "Password is required")
    private String password;
}
