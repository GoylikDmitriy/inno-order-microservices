package com.goylik.auth_service.model.dto.request;

import com.goylik.auth_service.model.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SaveCredentialsRequest(
        @NotNull Long userId,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotNull Role role
) {
}
