package com.goylik.user_service.controller.advice;

import com.goylik.user_service.exception.user.AccessDeniedException;
import com.goylik.user_service.exception.user.UserAlreadyExistsException;
import com.goylik.user_service.exception.user.UserNotFoundException;
import com.goylik.user_service.model.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class UserControllerAdvice {
    @ExceptionHandler(UserNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "User not found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "User already exists",
                ex.getMessage()
        );
    }

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
}
