package com.goylik.user_service.controller.advice;

import com.goylik.user_service.exception.card.*;
import com.goylik.user_service.model.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class CardControllerAdvice {
    @ExceptionHandler(CardNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleCardNotFoundException(CardNotFoundException ex) {
        log.warn("Card not found: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Card not found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(CardLimitExceededException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCardLimitExceededException(CardLimitExceededException ex) {
        log.warn("Card limit exceeded: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Card limit exceeded",
                ex.getMessage()
        );
    }

    @ExceptionHandler(InvalidCardNumberException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleInvalidCardNumberException(InvalidCardNumberException ex) {
        log.warn("Invalid card number: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Invalid card number",
                ex.getMessage()
        );
    }

    @ExceptionHandler(CardCryptoException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCardCryptoException(CardCryptoException ex) {
        log.warn("Card crypto exception: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Card crypto exception",
                ex.getMessage()
        );
    }

    @ExceptionHandler(CardHashingException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleCardHashingException(CardHashingException ex) {
        log.warn("Card hashing exception: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.BAD_REQUEST.value(),
                "Card hashing exception",
                ex.getMessage()
        );
    }
}
