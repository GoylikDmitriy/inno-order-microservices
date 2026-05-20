package com.goylik.order_service.security.util;

import com.goylik.order_service.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("orderSecurity")
@RequiredArgsConstructor
public class OrderSecurityExpressions {
    private final OrderRepository orderRepository;

    public boolean isOrderOwner(Long orderId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return orderRepository.findById(orderId)
                .map(order -> order.getUserId().equals(currentUserId))
                .orElse(false);
    }
}
