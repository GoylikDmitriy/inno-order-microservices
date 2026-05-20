package com.goylik.user_service.client.fallback;

import com.goylik.user_service.exception.client.AuthServiceUnavailableException;
import com.goylik.user_service.model.dto.client.SaveCredentialsRequest;
import com.goylik.user_service.model.enums.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class AuthServiceClientFallbackTest {
    private AuthServiceClientFallback fallback;

    @BeforeEach
    void setUp() {
        fallback = new AuthServiceClientFallback();
    }

    @Test
    void saveCredentials_ShouldThrowAuthServiceUnavailableException() {
        SaveCredentialsRequest request = new SaveCredentialsRequest(
                1L,
                "john@test.com",
                "password",
                Role.ROLE_USER
        );

        assertThrows(
                AuthServiceUnavailableException.class,
                () -> fallback.saveCredentials(request)
        );
    }

    @Test
    void saveCredentials_ShouldThrowWithCorrectMessage() {
        SaveCredentialsRequest request = new SaveCredentialsRequest(
                1L,
                "john@test.com",
                "password",
                Role.ROLE_USER
        );

        AuthServiceUnavailableException ex = assertThrows(
                AuthServiceUnavailableException.class,
                () -> fallback.saveCredentials(request)
        );

        assertEquals("Auth service is unavailable.", ex.getMessage());
    }
}
