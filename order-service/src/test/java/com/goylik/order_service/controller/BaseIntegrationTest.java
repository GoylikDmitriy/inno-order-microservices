package com.goylik.order_service.controller;

import com.goylik.order_service.security.UserPrincipal;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres =
            new PostgreSQLContainer("postgres:16");

    protected MockHttpServletRequestBuilder withAdmin(MockHttpServletRequestBuilder request) {
        return request.with(SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(1L, "ROLE_ADMIN"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        ));
    }

    protected MockHttpServletRequestBuilder withUser(Long userId, MockHttpServletRequestBuilder request) {
        return request.with(SecurityMockMvcRequestPostProcessors.authentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(userId, "ROLE_USER"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        ));
    }
}
