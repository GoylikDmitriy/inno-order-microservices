package com.goylik.user_service.client.fallback;

import com.goylik.user_service.client.AuthServiceClient;
import com.goylik.user_service.exception.client.AuthServiceUnavailableException;
import com.goylik.user_service.model.dto.client.SaveCredentialsRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class AuthServiceClientFallback implements AuthServiceClient {
    @Override
    public void saveCredentials(SaveCredentialsRequest request) {
        log.warn("Can't get access to the auth service. User wasn't saved with email = {}", request.email());
        throw new AuthServiceUnavailableException("Auth service is unavailable.");
    }
}
