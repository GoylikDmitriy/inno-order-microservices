package com.goylik.order_service.model.dto.request;

import com.goylik.order_service.model.enums.OrderStatus;
import jakarta.validation.Valid;

import java.util.List;

public record UpdateOrderRequest(
        OrderStatus status,
        List<@Valid OrderItemRequest> items
) {}
