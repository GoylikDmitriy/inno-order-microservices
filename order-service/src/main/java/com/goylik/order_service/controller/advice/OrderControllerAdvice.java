package com.goylik.order_service.controller.advice;

import com.goylik.order_service.exception.ItemNotFoundException;
import com.goylik.order_service.exception.OrderNotFoundException;
import com.goylik.order_service.model.dto.response.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class OrderControllerAdvice {
    @ExceptionHandler(OrderNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleOrderNotFoundException(OrderNotFoundException ex) {
        log.warn("Order not found: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Order not found",
                ex.getMessage()
        );
    }

    @ExceptionHandler(ItemNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleItemNotFoundException(ItemNotFoundException ex) {
        log.warn("Item not found: {}", ex.getMessage());
        return ErrorResponse.of(
                HttpStatus.NOT_FOUND.value(),
                "Item not found",
                ex.getMessage()
        );
    }
}
