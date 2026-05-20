package com.goylik.order_service.model.dto.response;

import com.goylik.order_service.model.dto.client.UserResponse;
import com.goylik.order_service.model.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderResponse(
        Long id,
        Long userId,
        UserResponse user,
        OrderStatus status,
        BigDecimal totalPrice,
        List<OrderItemResponse> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
