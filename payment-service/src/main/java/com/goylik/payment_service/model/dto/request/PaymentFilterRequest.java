package com.goylik.payment_service.model.dto.request;

import com.goylik.payment_service.exception.SingleFilterRequiredException;
import com.goylik.payment_service.model.enums.PaymentStatus;

import java.util.Objects;
import java.util.stream.Stream;

public record PaymentFilterRequest(
        Long userId,
        Long orderId,
        PaymentStatus status
) {
    public PaymentFilterRequest {
        long filledFields = Stream.of(userId, orderId, status)
                .filter(Objects::nonNull)
                .count();

        if (filledFields != 1) {
            throw new SingleFilterRequiredException(
                    "Exactly one filter must be provided: userId, orderId or status");
        }
    }
}