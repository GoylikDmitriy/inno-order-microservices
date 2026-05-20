package com.goylik.order_service.controller.advice;

import com.goylik.order_service.client.UserServiceClient;
import com.goylik.order_service.controller.BaseIntegrationTest;
import com.goylik.order_service.exception.client.UserServiceUnavailableException;
import com.goylik.order_service.model.entity.Item;
import com.goylik.order_service.model.entity.Order;
import com.goylik.order_service.model.entity.OrderItem;
import com.goylik.order_service.repository.ItemRepository;
import com.goylik.order_service.repository.OrderRepository;
import feign.FeignException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class ClientControllerAdviceTest extends BaseIntegrationTest {
    @Autowired private OrderRepository orderRepository;
    @Autowired private ItemRepository itemRepository;
    @MockitoBean private UserServiceClient userServiceClient;

    private static final String BASE_URL = "/api/orders";
    private static final Long USER_ID = 100L;

    private Item item1;
    private Item item2;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();

        item1 = createItem("Product 1", BigDecimal.valueOf(99.99));
        item2 = createItem("Product 2", BigDecimal.valueOf(49.99));
        itemRepository.saveAll(List.of(item1, item2));
    }

    private Item createItem(String name, BigDecimal price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(price);
        return item;
    }

    private Order createTestOrder() {
        Order order = Order.create(USER_ID);
        order.setItems(List.of(
                OrderItem.create(order, item1, 1),
                OrderItem.create(order, item2, 1)
        ));
        return orderRepository.save(order);
    }

    @Test
    void shouldReturn503_WhenUserServiceThrowsUserServiceUnavailableException() throws Exception {
        Order order = createTestOrder();

        when(userServiceClient.getUserByIdInternal(USER_ID))
                .thenThrow(new UserServiceUnavailableException("User service is unavailable"));

        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/" + order.getId())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("User Service is unavailable"));
    }

    @Test
    void shouldReturn503_WhenUserServiceThrowsFeignException() throws Exception {
        Order order = createTestOrder();

        when(userServiceClient.getUserByIdInternal(USER_ID))
                .thenThrow(FeignException.errorStatus("getUserByIdInternal",
                        feign.Response.builder()
                                .status(503)
                                .reason("Service Unavailable")
                                .request(feign.Request.create(
                                        feign.Request.HttpMethod.GET,
                                        "/api/users/internal/" + USER_ID,
                                        java.util.Collections.emptyMap(),
                                        null,
                                        null,
                                        null))
                                .build()));

        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/" + order.getId())))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.status").value(503))
                .andExpect(jsonPath("$.error").value("User service error"));
    }

    @Test
    void shouldReturn404_WhenUserServiceReturnsNotFound() throws Exception {
        Order order = createTestOrder();

        when(userServiceClient.getUserByIdInternal(USER_ID))
                .thenThrow(FeignException.errorStatus("getUserByIdInternal",
                        feign.Response.builder()
                                .status(404)
                                .reason("Not Found")
                                .request(feign.Request.create(
                                        feign.Request.HttpMethod.GET,
                                        "/api/users/internal/" + USER_ID,
                                        java.util.Collections.emptyMap(),
                                        null,
                                        null,
                                        null))
                                .build()));

        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/" + order.getId())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource not found"));
    }

    @Test
    void shouldReturn400_WhenUserServiceReturnsBadRequest() throws Exception {
        Order order = createTestOrder();

        when(userServiceClient.getUserByIdInternal(USER_ID))
                .thenThrow(FeignException.errorStatus("getUserByIdInternal",
                        feign.Response.builder()
                                .status(400)
                                .reason("Bad Request")
                                .request(feign.Request.create(
                                        feign.Request.HttpMethod.GET,
                                        "/api/users/internal/" + USER_ID,
                                        java.util.Collections.emptyMap(),
                                        null,
                                        null,
                                        null))
                                .build()));

        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/" + order.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad request"));
    }

    @Test
    void shouldReturn409_WhenUserServiceReturnsConflict() throws Exception {
        Order order = createTestOrder();

        when(userServiceClient.getUserByIdInternal(USER_ID))
                .thenThrow(FeignException.errorStatus("getUserByIdInternal",
                        feign.Response.builder()
                                .status(409)
                                .reason("Conflict")
                                .request(feign.Request.create(
                                        feign.Request.HttpMethod.GET,
                                        "/api/users/internal/" + USER_ID,
                                        java.util.Collections.emptyMap(),
                                        null,
                                        null,
                                        null))
                                .build()));

        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/" + order.getId())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.error").value("Conflict"));
    }
}
