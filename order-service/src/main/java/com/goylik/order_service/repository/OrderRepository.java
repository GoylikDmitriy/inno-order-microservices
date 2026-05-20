package com.goylik.order_service.repository;

import com.goylik.order_service.model.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long>, JpaSpecificationExecutor<Order> {
    @Query("""
            SELECT o FROM Order o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.item
            WHERE o.id = :id
    """)
    Optional<Order> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT o.id FROM Order o WHERE o.userId = :userId")
    Page<Long> findAllOrderIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("""
            SELECT DISTINCT o FROM Order o
            LEFT JOIN FETCH o.items i
            LEFT JOIN FETCH i.item
            WHERE o.id IN :ids
    """)
    List<Order> findOrdersWithItemsByIds(@Param("ids") List<Long> ids);
}
