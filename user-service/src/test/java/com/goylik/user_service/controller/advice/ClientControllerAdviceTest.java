package com.goylik.user_service.controller.advice;

import com.goylik.user_service.client.AuthServiceClient;
import com.goylik.user_service.controller.BaseIntegrationTest;
import com.goylik.user_service.exception.client.AuthServiceUnavailableException;
import com.goylik.user_service.service.UserService;
import feign.FeignException;
import feign.Request;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ClientControllerAdviceTest extends BaseIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuthServiceClient authServiceClient;

    private static final String BASE_URL = "/api/users";
    private static final String VALID_USER_JSON = """
            {
                "name": "John",
                "surname": "Doe",
                "birthDate": "2000-11-05",
                "email": "john@test.com",
                "password": "password"
            }
            """;

    @Test
    void createUser_ShouldReturn503_WhenAuthServiceUnavailable() throws Exception {
        when(userService.createUser(any(), any()))
                .thenThrow(new AuthServiceUnavailableException("Auth service is unavailable."));

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Auth Service is unavailable"));
    }

    @Test
    void createUser_ShouldReturn409_WhenFeignConflict() throws Exception {
        when(userService.createUser(any(), any()))
                .thenThrow(new FeignException.Conflict(
                        "Email already taken",
                        Request.create(
                                Request.HttpMethod.POST,
                                "/api/auth/save-credentials",
                                Map.of(),
                                null,
                                null,
                                null
                        ),
                        null,
                        null
                ));

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Email already taken"));
    }

    @Test
    void createUser_ShouldReturn503_WhenFeignException() throws Exception {
        when(userService.createUser(any(), any()))
                .thenThrow(new FeignException.ServiceUnavailable(
                        "Auth service error",
                        Request.create(
                                Request.HttpMethod.POST,
                                "/api/auth/save-credentials",
                                Map.of(),
                                null,
                                null,
                                null
                        ),
                        null,
                        null
                ));

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Auth service error"));
    }

    @Test
    void createAdmin_ShouldReturn503_WhenAuthServiceUnavailable() throws Exception {
        when(userService.createUser(any(), any()))
                .thenThrow(new AuthServiceUnavailableException("Auth service is unavailable."));

        mockMvc.perform(withAdmin(post(BASE_URL + "/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON)))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("Auth Service is unavailable"));
    }

    @Test
    void createAdmin_ShouldReturn409_WhenFeignConflict() throws Exception {
        when(userService.createUser(any(), any()))
                .thenThrow(new FeignException.Conflict(
                        "Email already taken",
                        Request.create(
                                Request.HttpMethod.POST,
                                "/api/auth/save-credentials",
                                Map.of(),
                                null,
                                null,
                                null
                        ),
                        null,
                        null
                ));

        mockMvc.perform(withAdmin(post(BASE_URL + "/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Email already taken"));
    }
}