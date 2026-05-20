package com.goylik.order_service.controller.advice;

import com.goylik.order_service.exception.client.UserServiceUnavailableException;
import com.goylik.order_service.model.dto.response.ErrorResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class ClientControllerAdvice {
    @ExceptionHandler(UserServiceUnavailableException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleUserServiceUnavailableException(UserServiceUnavailableException ex) {
        log.warn("User service is unavailable: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "User Service is unavailable",
                ex.getMessage()
        );
    }

    @ExceptionHandler(FeignException.NotFound.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(FeignException.NotFound ex) {
        log.debug("Resource not found: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Resource not found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(FeignException.BadRequest.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleBadRequest(FeignException.BadRequest ex) {
        log.debug("Bad request: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Bad request",
                ex.getMessage()
        );
    }

    @ExceptionHandler(FeignException.Conflict.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleConflict(FeignException.Conflict ex) {
        log.debug("Conflict: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Conflict",
                ex.getMessage()
        );
    }

    @ExceptionHandler(FeignException.class)
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
    public ErrorResponse handleFeignException(FeignException ex) {
        log.warn("User service error: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.SERVICE_UNAVAILABLE.value(),
                "User service error",
                ex.getMessage()
        );
    }
}