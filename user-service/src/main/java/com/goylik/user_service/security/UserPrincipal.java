package com.goylik.user_service.security;

public record UserPrincipal(
        Long userId,
        String role
) {
}
