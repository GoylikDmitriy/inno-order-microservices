package com.goylik.order_service.controller;

import com.goylik.order_service.client.UserServiceClient;
import com.goylik.order_service.model.dto.client.UserResponse;
import com.goylik.order_service.model.entity.Item;
import com.goylik.order_service.model.entity.Order;
import com.goylik.order_service.model.entity.OrderItem;
import com.goylik.order_service.model.enums.OrderStatus;
import com.goylik.order_service.repository.ItemRepository;
import com.goylik.order_service.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class OrderControllerTest extends BaseIntegrationTest {
    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ItemRepository itemRepository;

    @MockitoBean
    protected UserServiceClient userServiceClient;

    private static final String BASE_URL = "/api/orders";

    private Item item1;
    private Item item2;
    private static final Long USER_ID = 100L;
    private static final Long ADMIN_USER_ID = 1L;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
        itemRepository.deleteAll();

        item1 = createItem("Product 1", BigDecimal.valueOf(99.99));
        item2 = createItem("Product 2", BigDecimal.valueOf(49.99));

        item1 = itemRepository.save(item1);
        item2 = itemRepository.save(item2);
    }

    private Item createItem(String name, BigDecimal price) {
        Item item = new Item();
        item.setName(name);
        item.setPrice(price);
        return item;
    }

    private Order createTestOrder(Long userId) {
        Order order = Order.create(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(BigDecimal.valueOf(149.98));

        OrderItem orderItem1 = OrderItem.create(order, item1, 1);
        OrderItem orderItem2 = OrderItem.create(order, item2, 1);
        order.setItems(List.of(orderItem1, orderItem2));

        return orderRepository.save(order);
    }

    private void mockUserExists(Long userId) {
        var response = new UserResponse(
                userId,
                "Doe",
                "John",
                LocalDate.of(1999, 10, 2),
                "john@test.com",
                true);

        when(userServiceClient.getUserByIdInternal(userId)).thenReturn(ResponseEntity.ok(response));
    }

    private void mockUsersBatch(List<Long> userIds) {
        List<UserResponse> users = userIds.stream()
                .map(id -> new UserResponse(
                        id,
                        "Doe",
                        "John",
                        LocalDate.of(1999, 10, 2),
                        "john@test.com",
                        true))
                .toList();

        when(userServiceClient.getUsersByIdsInternal(userIds)).thenReturn(ResponseEntity.ok(users));
    }

    @Test
    void createOrder_ShouldReturn201_WhenValidRequest() throws Exception {
        String requestJson = """
                {
                    "userId": %d,
                    "items": [
                        {"itemId": %d, "quantity": 2},
                        {"itemId": %d, "quantity": 1}
                    ]
                }
                """.formatted(USER_ID, item1.getId(), item2.getId());

        mockUserExists(USER_ID);

        mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalPrice").value(249.97))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].itemId").value(item1.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(2))
                .andExpect(jsonPath("$.items[1].itemId").value(item2.getId()))
                .andExpect(jsonPath("$.items[1].quantity").value(1));
    }

    @Test
    void createOrder_ShouldReturn400_WhenItemsEmpty() throws Exception {
        String requestJson = """
                {
                    "userId": %d,
                    "items": []
                }
                """.formatted(USER_ID);

        mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createOrder_ShouldReturn404_WhenItemNotFound() throws Exception {
        String requestJson = """
                {
                    "userId": %d,
                    "items": [
                        {"itemId": 999, "quantity": 1}
                    ]
                }
                """.formatted(USER_ID);

        mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Item not found with id = 999")));
    }

    @Test
    void createOrder_ShouldReturn403_WhenUserTriesToCreateForAnotherUser() throws Exception {
        String requestJson = """
                {
                    "userId": %d,
                    "items": [
                        {"itemId": %d, "quantity": 1}
                    ]
                }
                """.formatted(ADMIN_USER_ID, item1.getId());

        mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createOrder_ShouldReturn201_WhenAdminCreatesForAnyUser() throws Exception {
        String requestJson = """
                {
                    "userId": %d,
                    "items": [
                        {"itemId": %d, "quantity": 1}
                    ]
                }
                """.formatted(ADMIN_USER_ID, item1.getId());

        mockUserExists(ADMIN_USER_ID);

        mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestJson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(ADMIN_USER_ID));
    }

    @Test
    void getOrderById_ShouldReturn200_WhenUserIsOwner() throws Exception {
        Order order = createTestOrder(USER_ID);
        mockUserExists(USER_ID);

        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/" + order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.items").isArray())
                .andExpect(jsonPath("$.items.length()").value(2));
    }

    @Test
    void getOrderById_ShouldReturn200_WhenAdminRequests() throws Exception {
        Order order = createTestOrder(USER_ID);

        mockUserExists(USER_ID);

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + order.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(order.getId()))
                .andExpect(jsonPath("$.userId").value(USER_ID));
    }

    @Test
    void getOrderById_ShouldReturn403_WhenUserRequestsOtherUsersOrder() throws Exception {
        Order order = createTestOrder(USER_ID);

        mockMvc.perform(withUser(ADMIN_USER_ID, get(BASE_URL + "/" + order.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrderById_ShouldReturn404_WhenOrderDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/99999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Order not found with id = 99999")));
    }

    @Test
    void getOrderById_ShouldReturn400_WhenIdIsNegative() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllOrders_ShouldReturnPage_WithDefaultPagination() throws Exception {
        createTestOrder(USER_ID);
        createTestOrder(USER_ID);
        createTestOrder(ADMIN_USER_ID);

        mockUsersBatch(List.of(USER_ID, ADMIN_USER_ID));

        mockMvc.perform(withAdmin(get(BASE_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20));
    }

    @Test
    void getAllOrders_ShouldReturn403_WhenUserTriesToGetAll() throws Exception {
        mockMvc.perform(withUser(USER_ID, get(BASE_URL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllOrders_ShouldFilterByStatuses() throws Exception {
        Order order1 = createTestOrder(USER_ID);
        order1.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order1);

        Order order2 = createTestOrder(USER_ID);
        order2.setStatus(OrderStatus.PENDING);
        orderRepository.save(order2);

        mockUsersBatch(List.of(USER_ID));

        mockMvc.perform(withAdmin(get(BASE_URL).param("statuses", "CONFIRMED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("CONFIRMED"));
    }

    @Test
    void getAllOrders_ShouldFilterByMultipleStatuses() throws Exception {
        Order order1 = createTestOrder(USER_ID);
        order1.setStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order1);

        Order order2 = createTestOrder(USER_ID);
        order2.setStatus(OrderStatus.PENDING);
        orderRepository.save(order2);

        Order order3 = createTestOrder(USER_ID);
        order3.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order3);

        mockUsersBatch(List.of(USER_ID));

        mockMvc.perform(withAdmin(get(BASE_URL)
                        .param("statuses", "CONFIRMED,PENDING")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getAllOrders_ShouldReturnEmptyPage_WhenNoMatchFound() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL).param("statuses", "COMPLETED")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllOrders_ShouldRespectPagination() throws Exception {
        for (int i = 0; i < 15; i++) {
            createTestOrder(USER_ID);
        }

        mockUsersBatch(List.of(USER_ID));

        mockMvc.perform(withAdmin(get(BASE_URL)
                        .param("page", "1")
                        .param("size", "5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.pageable.pageNumber").value(1))
                .andExpect(jsonPath("$.pageable.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void getAllOrders_ShouldSortByCreatedAtDesc() throws Exception {
        Order order1 = createTestOrder(USER_ID);
        order1.setCreatedAt(LocalDateTime.now().minusDays(5));
        orderRepository.save(order1);

        Order order2 = createTestOrder(USER_ID);
        order2.setCreatedAt(LocalDateTime.now().minusDays(1));
        orderRepository.save(order2);

        Order order3 = createTestOrder(USER_ID);
        order3.setCreatedAt(LocalDateTime.now());
        orderRepository.save(order3);

        mockUsersBatch(List.of(USER_ID));

        mockMvc.perform(withAdmin(get(BASE_URL)
                        .param("sort", "createdAt,desc")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].createdAt").isNotEmpty());
    }

    @Test
    void getOrdersByUserId_ShouldReturn200_WhenUserRequestsOwnOrders() throws Exception {
        createTestOrder(USER_ID);
        createTestOrder(USER_ID);

        mockUsersBatch(List.of(USER_ID));

        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/users/" + USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].userId").value(USER_ID));
    }

    @Test
    void getOrdersByUserId_ShouldReturn200_WhenAdminRequests() throws Exception {
        createTestOrder(USER_ID);
        createTestOrder(USER_ID);

        mockUsersBatch(List.of(USER_ID));

        mockMvc.perform(withAdmin(get(BASE_URL + "/users/" + USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    void getOrdersByUserId_ShouldReturn403_WhenUserRequestsOtherUsersOrders() throws Exception {
        createTestOrder(USER_ID);

        mockMvc.perform(withUser(ADMIN_USER_ID, get(BASE_URL + "/users/" + USER_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getOrdersByUserId_ShouldReturnEmptyPage_WhenUserHasNoOrders() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/users/" + 888L)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void updateOrder_ShouldReturn200_WhenUserUpdatesOwnOrderStatus() throws Exception {
        Order order = createTestOrder(USER_ID);

        String updateJson = """
                {
                    "status": "CONFIRMED"
                }
                """;

        mockUserExists(USER_ID);

        mockMvc.perform(withUser(USER_ID, put(BASE_URL + "/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }

    @Test
    void updateOrder_ShouldReturn200_WhenUserUpdatesOwnOrderItems() throws Exception {
        Order order = createTestOrder(USER_ID);

        String updateJson = """
                {
                    "items": [
                        {"itemId": %d, "quantity": 5}
                    ]
                }
                """.formatted(item1.getId());

        mockUserExists(USER_ID);

        mockMvc.perform(withUser(USER_ID, put(BASE_URL + "/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].itemId").value(item1.getId()))
                .andExpect(jsonPath("$.items[0].quantity").value(5))
                .andExpect(jsonPath("$.totalPrice").value(499.95));
    }

    @Test
    void updateOrder_ShouldReturn200_WhenAdminUpdatesAnyOrder() throws Exception {
        Order order = createTestOrder(USER_ID);

        String updateJson = """
                {
                    "status": "COMPLETED"
                }
                """;

        mockUserExists(USER_ID);

        mockMvc.perform(withAdmin(put(BASE_URL + "/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void updateOrder_ShouldReturn403_WhenUserUpdatesOtherUsersOrder() throws Exception {
        Order order = createTestOrder(USER_ID);

        String updateJson = """
                {
                    "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(withUser(ADMIN_USER_ID, put(BASE_URL + "/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateOrder_ShouldReturn404_WhenOrderDoesNotExist() throws Exception {
        String updateJson = """
                {
                    "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Order not found with id = 99999")));
    }

    @Test
    void updateOrder_ShouldReturn404_WhenItemNotFound() throws Exception {
        Order order = createTestOrder(USER_ID);

        String updateJson = """
                {
                    "items": [
                        {"itemId": 999, "quantity": 1}
                    ]
                }
                """;

        mockMvc.perform(withUser(USER_ID, put(BASE_URL + "/" + order.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Item not found with id = 999")));
    }

    @Test
    void deleteOrder_ShouldReturn204_WhenUserDeletesOwnOrder() throws Exception {
        Order order = createTestOrder(USER_ID);

        mockMvc.perform(withUser(USER_ID, delete(BASE_URL + "/" + order.getId())))
                .andExpect(status().isNoContent());

        assertThat(orderRepository.findById(order.getId())).isEmpty();
    }

    @Test
    void deleteOrder_ShouldReturn204_WhenAdminDeletesAnyOrder() throws Exception {
        Order order = createTestOrder(USER_ID);

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + order.getId())))
                .andExpect(status().isNoContent());

        assertThat(orderRepository.findById(order.getId())).isEmpty();
    }

    @Test
    void deleteOrder_ShouldReturn403_WhenUserDeletesOtherUsersOrder() throws Exception {
        Order order = createTestOrder(USER_ID);

        mockMvc.perform(withUser(ADMIN_USER_ID, delete(BASE_URL + "/" + order.getId())))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteOrder_ShouldReturn404_WhenOrderDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(delete(BASE_URL + "/99999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Order not found with id = 99999")));
    }

    @Test
    void deleteOrder_ShouldReturn404_WhenOrderAlreadyDeleted() throws Exception {
        Order order = createTestOrder(USER_ID);

        mockMvc.perform(withUser(USER_ID, delete(BASE_URL + "/" + order.getId())))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + order.getId())))
                .andExpect(status().isNotFound());
    }

    @Test
    void completeOrderLifecycle_ShouldWork() throws Exception {
        String createJson = """
                {
                    "userId": %d,
                    "items": [
                        {"itemId": %d, "quantity": 2},
                        {"itemId": %d, "quantity": 1}
                    ]
                }
                """.formatted(USER_ID, item1.getId(), item2.getId());

        mockUserExists(USER_ID);

        MvcResult createResult = mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createJson)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdOrder = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long orderId = createdOrder.get("id").asLong();

        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/" + orderId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));

        String updateJson = """
                {
                    "status": "CONFIRMED"
                }
                """;

        mockMvc.perform(withUser(USER_ID, put(BASE_URL + "/" + orderId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        mockUsersBatch(List.of(USER_ID));
        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/users/" + USER_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(orderId));

        mockMvc.perform(withUser(USER_ID, delete(BASE_URL + "/" + orderId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + orderId)))
                .andExpect(status().isNotFound());
    }
}
