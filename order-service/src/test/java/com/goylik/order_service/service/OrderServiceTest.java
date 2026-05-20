package com.goylik.order_service.service;

import com.goylik.order_service.client.UserServiceClient;
import com.goylik.order_service.exception.ItemNotFoundException;
import com.goylik.order_service.exception.OrderNotFoundException;
import com.goylik.order_service.mapper.OrderMapper;
import com.goylik.order_service.model.dto.client.UserResponse;
import com.goylik.order_service.model.dto.request.CreateOrderRequest;
import com.goylik.order_service.model.dto.request.FilterRequest;
import com.goylik.order_service.model.dto.request.OrderItemRequest;
import com.goylik.order_service.model.dto.request.UpdateOrderRequest;
import com.goylik.order_service.model.dto.response.OrderItemResponse;
import com.goylik.order_service.model.dto.response.OrderResponse;
import com.goylik.order_service.model.entity.Item;
import com.goylik.order_service.model.entity.Order;
import com.goylik.order_service.model.entity.OrderItem;
import com.goylik.order_service.model.enums.OrderStatus;
import com.goylik.order_service.repository.ItemRepository;
import com.goylik.order_service.repository.OrderRepository;
import com.goylik.order_service.service.impl.OrderTransactionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {
    @Mock private OrderRepository orderRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private OrderMapper orderMapper;
    @Mock private UserServiceClient userServiceClient;

    @InjectMocks
    private OrderTransactionService orderService;

    private static final Long ORDER_ID = 1L;
    private static final Long USER_ID = 100L;
    private static final Long ITEM_ID_1 = 10L;
    private static final Long ITEM_ID_2 = 11L;

    private Order order;
    private OrderResponse orderResponse;
    private UserResponse userResponse;
    private Item item1;
    private Item item2;
    private OrderItem orderItem1;
    private OrderItem orderItem2;

    @BeforeEach
    void setUp() {
        userResponse = new UserResponse(
                USER_ID,
                "Doe",
                "John",
                LocalDate.of(1999, 10, 2),
                "john@test.com",
                true
        );

        order = Order.create(USER_ID);
        order.setId(ORDER_ID);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalPrice(BigDecimal.valueOf(150.00));
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        item1 = new Item();
        item1.setId(ITEM_ID_1);
        item1.setName("Product 1");
        item1.setPrice(BigDecimal.valueOf(100.00));

        item2 = new Item();
        item2.setId(ITEM_ID_2);
        item2.setName("Product 2");
        item2.setPrice(BigDecimal.valueOf(50.00));

        orderItem1 = OrderItem.create(order, item1, 1);
        orderItem2 = OrderItem.create(order, item2, 1);
        order.setItems(List.of(orderItem1, orderItem2));

        OrderItemResponse orderItemResponse1 = new OrderItemResponse(
                1L, ITEM_ID_1, "Product 1", BigDecimal.valueOf(100.00), 1
        );
        OrderItemResponse orderItemResponse2 = new OrderItemResponse(
                2L, ITEM_ID_2, "Product 2", BigDecimal.valueOf(50.00), 1
        );

        orderResponse = new OrderResponse(
                ORDER_ID,
                USER_ID,
                userResponse,
                OrderStatus.PENDING,
                BigDecimal.valueOf(150.00),
                List.of(orderItemResponse1, orderItemResponse2),
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    @Test
    void createOrder_ShouldReturnOrderResponse_WhenValidRequest() {
        CreateOrderRequest request = createOrderRequest();

        when(itemRepository.findAllById(List.of(ITEM_ID_1, ITEM_ID_2)))
                .thenReturn(List.of(item1, item2));
        when(orderRepository.save(any(Order.class))).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.createOrder(request);

        assertThat(result).isNotNull();
        assertThat(result.userId()).isEqualTo(USER_ID);
        assertThat(result.user()).isEqualTo(userResponse);
        verify(orderRepository).save(any(Order.class));
        verify(itemRepository).findAllById(List.of(ITEM_ID_1, ITEM_ID_2));
    }

    @Test
    void createOrder_ShouldThrowItemNotFoundException_WhenItemNotFound() {
        CreateOrderRequest request = createOrderRequest();

        when(itemRepository.findAllById(List.of(ITEM_ID_1, ITEM_ID_2)))
                .thenReturn(List.of(item1));

        assertThatThrownBy(() -> orderService.createOrder(request))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("Item not found with id = " + ITEM_ID_2);

        verify(orderRepository, never()).save(any(Order.class));
    }

    @Test
    void getOrderById_ShouldReturnOrderResponse_WhenOrderExists() {
        when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        OrderResponse result = orderService.getOrderById(ORDER_ID);

        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(ORDER_ID);
        assertThat(result.user()).isEqualTo(userResponse);
        verify(orderRepository).findByIdWithItems(ORDER_ID);
    }

    @Test
    void getOrderById_ShouldThrowOrderNotFoundException_WhenOrderNotFound() {
        when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with id = " + ORDER_ID);
    }

    @Test
    void getAll_ShouldReturnPageOfOrderResponses() {
        Pageable pageable = Pageable.ofSize(10);
        FilterRequest filterRequest = new FilterRequest(null, null, null);
        List<Order> orders = List.of(order);
        Page<Order> ordersPage = new PageImpl<>(orders, pageable, 1);
        List<Long> ids = List.of(ORDER_ID);

        when(orderRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(ordersPage);
        when(orderRepository.findOrdersWithItemsByIds(ids)).thenReturn(orders);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        Page<OrderResponse> result = orderService.getAll(pageable, filterRequest);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).user()).isEqualTo(userResponse);
        verify(orderRepository).findAll(any(Specification.class), eq(pageable));
        verify(orderRepository).findOrdersWithItemsByIds(ids);
    }

    @Test
    void getOrdersByUserId_ShouldReturnPageOfOrderResponses() {
        Pageable pageable = Pageable.ofSize(10);
        Page<Long> idsPage = new PageImpl<>(List.of(ORDER_ID), pageable, 1);
        List<Order> orders = List.of(order);

        when(orderRepository.findAllOrderIdsByUserId(USER_ID, pageable)).thenReturn(idsPage);
        when(orderRepository.findOrdersWithItemsByIds(idsPage.getContent())).thenReturn(orders);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        Page<OrderResponse> result = orderService.getOrdersByUserId(USER_ID, pageable);

        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).userId()).isEqualTo(USER_ID);
        verify(orderRepository).findAllOrderIdsByUserId(USER_ID, pageable);
        verify(orderRepository).findOrdersWithItemsByIds(idsPage.getContent());
    }

    @Test
    void updateOrder_ShouldUpdateStatus_WhenStatusProvided() {
        UpdateOrderRequest request = new UpdateOrderRequest(OrderStatus.CONFIRMED, null);

        when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        orderService.updateOrder(ORDER_ID, request);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrder_ShouldUpdateItems_WhenItemsProvided() {
        List<OrderItemRequest> newItems = List.of(
                new OrderItemRequest(ITEM_ID_1, 2)
        );
        UpdateOrderRequest request = new UpdateOrderRequest(null, newItems);

        when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(order));
        when(itemRepository.findAllById(List.of(ITEM_ID_1))).thenReturn(List.of(item1));
        when(orderRepository.save(order)).thenReturn(order);
        when(orderMapper.toResponse(order)).thenReturn(orderResponse);

        orderService.updateOrder(ORDER_ID, request);

        verify(itemRepository).findAllById(List.of(ITEM_ID_1));
        verify(orderRepository).save(order);
    }

    @Test
    void updateOrder_ShouldThrowOrderNotFoundException_WhenOrderNotFound() {
        UpdateOrderRequest request = new UpdateOrderRequest(OrderStatus.CONFIRMED, null);

        when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrder(ORDER_ID, request))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with id = " + ORDER_ID);
    }

    @Test
    void updateOrder_ShouldThrowItemNotFoundException_WhenItemNotFound() {
        List<OrderItemRequest> newItems = List.of(
                new OrderItemRequest(999L, 2)
        );

        UpdateOrderRequest request = new UpdateOrderRequest(null, newItems);

        when(orderRepository.findByIdWithItems(ORDER_ID)).thenReturn(Optional.of(order));
        when(itemRepository.findAllById(List.of(999L))).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.updateOrder(ORDER_ID, request))
                .isInstanceOf(ItemNotFoundException.class)
                .hasMessageContaining("Item not found with id = 999");
    }

    @Test
    void deleteOrder_ShouldDeleteOrder_WhenOrderExists() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order));

        orderService.deleteOrder(ORDER_ID);

        verify(orderRepository).delete(order);
    }

    @Test
    void deleteOrder_ShouldThrowOrderNotFoundException_WhenOrderNotFound() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.deleteOrder(ORDER_ID))
                .isInstanceOf(OrderNotFoundException.class)
                .hasMessageContaining("Order not found with id = " + ORDER_ID);
    }

    private CreateOrderRequest createOrderRequest() {
        List<OrderItemRequest> items = List.of(
                new OrderItemRequest(ITEM_ID_1, 1),
                new OrderItemRequest(ITEM_ID_2, 1)
        );
        return new CreateOrderRequest(USER_ID, items);
    }
}
