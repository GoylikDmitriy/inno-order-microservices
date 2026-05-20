package com.goylik.auth_service.security.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class InternalApiKeyFilter extends OncePerRequestFilter {
    private final InternalApiKeysProperties apiKeysProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (request.getRequestURI().equals("/api/auth/save-credentials")) {
            String key = request.getHeader("X-Internal-Api-Key");
            if (key == null || !apiKeysProperties.getTrustedServices().contains(key)) {
                response.setStatus(HttpStatus.FORBIDDEN.value());
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
