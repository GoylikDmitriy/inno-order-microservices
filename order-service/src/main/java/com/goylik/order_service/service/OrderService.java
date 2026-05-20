package com.goylik.order_service.service;

import com.goylik.order_service.model.dto.request.CreateOrderRequest;
import com.goylik.order_service.model.dto.request.FilterRequest;
import com.goylik.order_service.model.dto.request.UpdateOrderRequest;
import com.goylik.order_service.model.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface OrderService {
    /**
     * Creates a new order for a user.
     * Calculates the total price based on the items and their quantities.
     *
     * @param request the order creation request containing user ID and list of items
     * @return the created order with user information
     * @throws com.goylik.order_service.exception.ItemNotFoundException if any item in the request does not exist
     */
    OrderResponse createOrder(CreateOrderRequest request);

    /**
     * Retrieves an order by its ID.
     * Fetches order items and enriches with user information from User Service.
     *
     * @param id the order ID
     * @return the order with user information
     * @throws com.goylik.order_service.exception.OrderNotFoundException if order with given ID does not exist
     */
    OrderResponse getOrderById(Long id);

    /**
     * Retrieves all orders with pagination and optional filtering.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @param request  filter parameters (date range, order statuses)
     * @return paginated list of orders with user information
     */
    Page<OrderResponse> getAll(Pageable pageable, FilterRequest request);

    /**
     * Retrieves all orders for a specific user with pagination.
     *
     * @param userId   the ID of the user
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of orders belonging to the specified user
     */
    Page<OrderResponse> getOrdersByUserId(Long userId, Pageable pageable);

    /**
     * Updates an existing order.
     * Can update the order status and/or the list of items.
     * When items are updated, the total price is recalculated automatically.
     *
     * @param id      the ID of the order to update
     * @param request the update request containing new status and/or items
     * @return the updated order with user information
     * @throws com.goylik.order_service.exception.OrderNotFoundException if order with given ID does not exist
     * @throws com.goylik.order_service.exception.ItemNotFoundException   if any item in the request does not exist
     */
    OrderResponse updateOrder(Long id, UpdateOrderRequest request);

    /**
     * Deletes an order by its ID.
     *
     * @param id the ID of the order to delete
     * @throws com.goylik.order_service.exception.OrderNotFoundException if order with given ID does not exist
     */
    void deleteOrder(Long id);
}
