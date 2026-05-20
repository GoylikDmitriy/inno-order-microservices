package com.goylik.payment_service.kafka.event;

import com.goylik.payment_service.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCreatedEvent(
        String paymentId,
        Long orderId,
        Long userId,
        PaymentStatus status,
        BigDecimal paymentAmount,
        LocalDateTime timestamp
) {
}
