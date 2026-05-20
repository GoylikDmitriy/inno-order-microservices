package com.goylik.order_service.service.impl;

import com.goylik.order_service.model.dto.request.CreateOrderRequest;
import com.goylik.order_service.model.dto.request.FilterRequest;
import com.goylik.order_service.model.dto.request.UpdateOrderRequest;
import com.goylik.order_service.model.dto.response.OrderResponse;
import com.goylik.order_service.service.OrderService;
import com.goylik.order_service.service.util.UserInfoEnricher;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {
    private final OrderTransactionService orderTransactionService;
    private final UserInfoEnricher userInfoEnricher;

    @Override
    public OrderResponse createOrder(CreateOrderRequest request) {
        var response = orderTransactionService.createOrder(request);
        return userInfoEnricher.enrich(response);
    }

    @Override
    public OrderResponse getOrderById(Long id) {
        var response = orderTransactionService.getOrderById(id);
        return userInfoEnricher.enrich(response);
    }

    @Override
    public Page<OrderResponse> getAll(Pageable pageable, FilterRequest request) {
        Page<OrderResponse> responses = orderTransactionService.getAll(pageable, request);
        return enrichPageWithUserInfo(pageable, responses);
    }

    private Page<OrderResponse> enrichPageWithUserInfo(Pageable pageable, Page<OrderResponse> responses) {
        if (responses.isEmpty()) {
            return Page.empty(pageable);
        }

        List<OrderResponse> responsesWithUserInfo = userInfoEnricher.enrichAll(responses.getContent());
        return new PageImpl<>(responsesWithUserInfo, pageable, responses.getTotalElements());
    }

    @Override
    public Page<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable) {
        Page<OrderResponse> responses = orderTransactionService.getOrdersByUserId(userId, pageable);
        return enrichPageWithUserInfo(pageable, responses);
    }

    @Override
    public OrderResponse updateOrder(Long id, UpdateOrderRequest request) {
        OrderResponse response = orderTransactionService.updateOrder(id, request);
        return userInfoEnricher.enrich(response);
    }

    @Override
    public void deleteOrder(Long id) {
        orderTransactionService.deleteOrder(id);
    }
}
