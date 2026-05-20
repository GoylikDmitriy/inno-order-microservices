package com.goylik.order_service.security;

public record UserPrincipal(
        Long userId,
        String role
) {
}
