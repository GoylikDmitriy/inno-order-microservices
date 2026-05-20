package com.goylik.order_service.controller.advice;

import com.goylik.order_service.exception.AccessDeniedException;
import com.goylik.order_service.model.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class SecurityControllerAdvice {
    @ExceptionHandler(AccessDeniedException.class)
    @ResponseStatus(HttpStatus.FORBIDDEN)
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex) {
        log.warn("Access denied: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.FORBIDDEN.value(),
                "Access denied",
                ex.getMessage()
        );
    }

    @ExceptionHandler(AuthenticationException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleAuthenticationException(AuthenticationException ex) {
        log.warn("Authentication failed: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Authentication failed",
                ex.getMessage()
        );
    }
}
