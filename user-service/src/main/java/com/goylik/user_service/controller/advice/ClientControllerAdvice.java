package com.goylik.user_service.controller.advice;

import com.goylik.user_service.exception.client.AuthServiceUnavailableException;
import com.goylik.user_service.model.dto.response.ErrorResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ClientControllerAdvice {
    @ExceptionHandler(AuthServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleAuthServiceUnavailableException(AuthServiceUnavailableException ex) {
        log.warn("Auth service is unavailable: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Auth Service is unavailable",
                ex.getMessage()
        );
    }

    @ExceptionHandler(FeignException.Conflict.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleFeignConflict(FeignException.Conflict ex) {
        log.warn("User with specified email already exists: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Email already taken",
                ex.getMessage()
        );
    }

    @ExceptionHandler(FeignException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleFeignException(FeignException ex) {
        log.warn("Auth service error: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "Auth service error",
                ex.getMessage()
        );
    }
}
