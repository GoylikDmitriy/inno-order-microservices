package com.goylik.order_service.kafka.event;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentCreatedEvent(
        String paymentId,
        Long orderId,
        Long userId,
        String status,
        BigDecimal paymentAmount,
        LocalDateTime timestamp
) {
}
