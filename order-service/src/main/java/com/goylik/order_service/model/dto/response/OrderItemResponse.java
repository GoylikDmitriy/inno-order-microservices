package com.goylik.order_service.model.dto.response;

import java.math.BigDecimal;

public record OrderItemResponse(
        Long id,
        Long itemId,
        String itemName,
        BigDecimal itemPrice,
        Integer quantity
) {}
