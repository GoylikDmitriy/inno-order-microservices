package com.goylik.auth_service.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public record ValidateTokenRequest(
        @NotBlank String token
) {
}
