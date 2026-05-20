package com.goylik.auth_service.model.dto.response;

public record TokenResponse(
        String accessToken,
        String refreshToken
) {
}
