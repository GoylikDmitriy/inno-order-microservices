package com.goylik.order_service.service.util;

import com.goylik.order_service.client.UserServiceClient;
import com.goylik.order_service.model.dto.client.UserResponse;
import com.goylik.order_service.model.dto.response.OrderResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserInfoEnricher {
    private final UserServiceClient userServiceClient;

    public OrderResponse enrich(OrderResponse response) {
        UserResponse user = fetchUserById(response.userId());
        return new OrderResponse(
                response.id(),
                response.userId(),
                user,
                response.status(),
                response.totalPrice(),
                response.items(),
                response.createdAt(),
                response.updatedAt()
        );
    }

    public List<OrderResponse> enrichAll(List<OrderResponse> responses) {
        if (responses.isEmpty()) {
            return responses;
        }

        List<Long> userIds = responses.stream()
                .map(OrderResponse::userId)
                .distinct()
                .toList();

        List<UserResponse> users = fetchUsersByIds(userIds);
        Map<Long, UserResponse> userMap = users.stream()
                .collect(Collectors.toMap(UserResponse::id, Function.identity()));

        return responses.stream()
                .map(resp -> new OrderResponse(
                        resp.id(), resp.userId(), userMap.get(resp.userId()),
                        resp.status(), resp.totalPrice(),
                        resp.items(), resp.createdAt(), resp.updatedAt()
                ))
                .toList();
    }

    private UserResponse fetchUserById(Long userId) {
        return userServiceClient.getUserByIdInternal(userId).getBody();
    }

    private List<UserResponse> fetchUsersByIds(List<Long> userIds) {
        return userServiceClient.getUsersByIdsInternal(userIds).getBody();
    }
}
