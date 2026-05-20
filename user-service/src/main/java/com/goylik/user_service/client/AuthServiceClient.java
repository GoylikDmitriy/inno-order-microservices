package com.goylik.user_service.client;

import com.goylik.user_service.client.config.AuthServiceFeignConfig;
import com.goylik.user_service.client.fallback.AuthServiceClientFallback;
import com.goylik.user_service.model.dto.client.SaveCredentialsRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "auth-service",
        url = "${client.gateway.url}",
        fallback = AuthServiceClientFallback.class,
        configuration = AuthServiceFeignConfig.class)
public interface AuthServiceClient {
    @PostMapping("/api/auth/save-credentials")
    void saveCredentials(@RequestBody SaveCredentialsRequest request);
}
