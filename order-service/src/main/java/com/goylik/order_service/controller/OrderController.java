package com.goylik.order_service.controller;

import com.goylik.order_service.model.dto.request.CreateOrderRequest;
import com.goylik.order_service.model.dto.request.FilterRequest;
import com.goylik.order_service.model.dto.request.UpdateOrderRequest;
import com.goylik.order_service.model.dto.response.OrderResponse;
import com.goylik.order_service.service.OrderService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing orders.
 * Provides endpoints for creating, retrieving, updating, and deleting orders.
 * Access is controlled based on user roles (ADMIN/USER).
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Validated
public class OrderController {
    private final OrderService orderService;

    /**
     * Creates a new order.
     * ADMIN can create an order for any user.
     * USER can only create an order for themselves.
     *
     * @param request the order creation request containing userId and items
     * @return the created order with user info
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or #request.userId() == authentication.principal.userId")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(orderService.createOrder(request));
    }

    /**
     * Retrieves an order by its ID.
     * ADMIN can retrieve any order.
     * USER can only retrieve their own orders.
     *
     * @param id the order ID
     * @return the order with user info
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOrderOwner(#id)")
    public ResponseEntity<OrderResponse> getOrderById(@Positive @PathVariable Long id) {
        return ResponseEntity.ok(orderService.getOrderById(id));
    }

    /**
     * Retrieves all orders with pagination and optional filters.
     * Only accessible by ADMIN.
     *
     * @param pageable pagination parameters (page, size, sort)
     * @param request  filter parameters (from, to, statuses)
     * @return paginated list of orders with user info
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<OrderResponse>> getAll(Pageable pageable,
                                      @ModelAttribute FilterRequest request) {
        return ResponseEntity.ok(orderService.getAll(pageable, request));
    }

    /**
     * Retrieves all orders for a specific user with pagination.
     * ADMIN can retrieve orders for any user.
     * USER can only retrieve their own orders.
     *
     * @param userId   the user ID
     * @param pageable pagination parameters (page, size, sort)
     * @return paginated list of orders with user info
     */
    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<Page<OrderResponse>> getOrdersByUserId(@Positive @PathVariable Long userId,
                                                 Pageable pageable) {
        return ResponseEntity.ok(orderService.getOrdersByUserId(userId, pageable));
    }

    /**
     * Updates an existing order by its ID.
     * ADMIN can update any order.
     * USER can only update their own orders.
     *
     * @param id      the order ID
     * @param request the update request containing new status and/or items
     * @return the updated order with user info
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOrderOwner(#id)")
    public ResponseEntity<OrderResponse> updateOrder(@Positive @PathVariable Long id,
                                     @Valid @RequestBody UpdateOrderRequest request) {
        return ResponseEntity.ok(orderService.updateOrder(id, request));
    }

    /**
     * Soft deletes an order by its ID.
     * ADMIN can delete any order.
     * USER can only delete their own orders.
     *
     * @param id the order ID
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN') or @orderSecurity.isOrderOwner(#id)")
    public ResponseEntity<Void> deleteOrder(@Positive @PathVariable Long id) {
        orderService.deleteOrder(id);
        return ResponseEntity.noContent().build();
    }
}