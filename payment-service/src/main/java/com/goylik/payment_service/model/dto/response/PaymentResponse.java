package com.goylik.payment_service.model.dto.response;

import com.goylik.payment_service.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentResponse(
        String id,
        Long orderId,
        Long userId,
        PaymentStatus status,
        LocalDateTime timestamp,
        BigDecimal paymentAmount
) {
}
