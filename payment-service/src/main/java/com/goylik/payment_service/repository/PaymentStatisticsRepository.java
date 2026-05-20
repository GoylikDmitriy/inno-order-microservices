package com.goylik.payment_service.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public interface PaymentStatisticsRepository {
    BigDecimal getTotalAmountByUserIdAndDateRange(Long userId, LocalDateTime from, LocalDateTime to);
    BigDecimal getTotalAmountByDateRange(LocalDateTime from, LocalDateTime to);
}
