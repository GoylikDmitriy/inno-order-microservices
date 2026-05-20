package com.goylik.payment_service.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TotalAmountResponse(
        BigDecimal totalAmount,
        LocalDateTime from,
        LocalDateTime to
) {
}
