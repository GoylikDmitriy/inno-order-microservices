package com.goylik.user_service.client.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;

public class AuthServiceFeignConfig {
    @Value("${app.internal.api-keys.user-service}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalApiKeyInterceptor() {
        return template -> template.header("X-Internal-Api-Key", internalApiKey);
    }
}
