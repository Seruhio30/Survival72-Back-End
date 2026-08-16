package com.seruhioCode30.survival72.controller.admin.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminLoginRequest(
        @NotBlank(message = "username is required")
        @Size(max = 120, message = "username must not exceed 120 characters")
        String username,

        @NotBlank(message = "password is required")
        @Size(max = 200, message = "password must not exceed 200 characters")
        String password
) {
}
