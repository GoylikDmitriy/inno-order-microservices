package com.goylik.user_service.model.dto.client;

import com.goylik.user_service.model.enums.Role;
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