package com.goylik.order_service.client;

import com.goylik.order_service.client.config.UserServiceFeignConfig;
import com.goylik.order_service.client.fallback.UserServiceClientFallback;
import com.goylik.order_service.model.dto.client.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(
        name = "user-service",
        url = "${client.gateway.url}",
        fallback = UserServiceClientFallback.class,
        configuration = UserServiceFeignConfig.class)
public interface UserServiceClient {
    @GetMapping("/api/users/internal/{id}")
    ResponseEntity<UserResponse> getUserByIdInternal(@PathVariable Long id);

    @GetMapping("/api/users/internal")
    ResponseEntity<List<UserResponse>> getUsersByIdsInternal(@RequestParam("id") List<Long> ids);
}
