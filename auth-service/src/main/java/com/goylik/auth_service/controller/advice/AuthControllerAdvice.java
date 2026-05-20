package com.goylik.auth_service.controller.advice;

import com.goylik.auth_service.exception.CredentialsAlreadyExistException;
import com.goylik.auth_service.exception.EmailAlreadyExistsException;
import com.goylik.auth_service.exception.InvalidTokenException;
import com.goylik.auth_service.exception.UserNotFoundException;
import com.goylik.auth_service.model.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class AuthControllerAdvice {
    @ExceptionHandler(CredentialsAlreadyExistException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleCredentialsAlreadyExistException(CredentialsAlreadyExistException ex) {
        log.warn("Credentials already exist: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Credentials already exist",
                ex.getMessage()
        );
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleEmailAlreadyExistsException(EmailAlreadyExistsException ex) {
        log.warn("Email is already taken: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.CONFLICT.value(),
                "Email is already taken",
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidTokenException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleInvalidTokenException(InvalidTokenException ex) {
        log.warn("Invalid token error: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid token",
                ex.getMessage()
        );
    }

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

    @ExceptionHandler(BadCredentialsException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleBadCredentials(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Invalid login or password",
                ex.getMessage()
        );
    }

    @ExceptionHandler(DisabledException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleDisabled(DisabledException ex) {
        log.warn("Account disabled: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Account is disabled",
                ex.getMessage()
        );
    }

    @ExceptionHandler(LockedException.class)
    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    public ErrorResponse handleLocked(LockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.UNAUTHORIZED.value(),
                "Account is locked",
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
