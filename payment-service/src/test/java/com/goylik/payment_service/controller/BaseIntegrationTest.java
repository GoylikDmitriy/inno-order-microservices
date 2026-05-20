package com.goylik.payment_service.controller;

import com.goylik.payment_service.security.UserPrincipal;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;

import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public abstract class BaseIntegrationTest {
    @Autowired
    protected MockMvc mockMvc;

    @Container
    @ServiceConnection
    static final MongoDBContainer mongodb = new MongoDBContainer("mongo:7-jammy");

    @Container
    @ServiceConnection
    static final KafkaContainer kafka = new KafkaContainer("apache/kafka:3.7.0");

    protected MockHttpServletRequestBuilder withAdmin(MockHttpServletRequestBuilder request) {
        return request.with(authentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(1L, "ROLE_ADMIN"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
                )
        ));
    }

    protected MockHttpServletRequestBuilder withUser(Long userId,
                                                     MockHttpServletRequestBuilder request) {
        return request.with(authentication(
                new UsernamePasswordAuthenticationToken(
                        new UserPrincipal(userId, "ROLE_USER"),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                )
        ));
    }
}
