package com.goylik.order_service.specification;

import com.goylik.order_service.model.entity.Order;
import com.goylik.order_service.model.enums.OrderStatus;
import jakarta.persistence.criteria.Path;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class OrderSpecification {
    public static Specification<Order> createdBetween(LocalDateTime from, LocalDateTime to) {
        return (root, query, cb) -> {
            Path<LocalDateTime> createdAt = root.get("createdAt");
            if (from == null && to == null) return cb.conjunction();
            if (from == null) return cb.lessThanOrEqualTo(createdAt, to);
            if (to == null) return cb.greaterThanOrEqualTo(createdAt, from);
            return cb.between(createdAt, from, to);
        };
    }

    public static Specification<Order> statusIn(List<OrderStatus> statuses) {
        return (root, query, cb) -> {
            if (statuses == null || statuses.isEmpty()) return cb.conjunction();
            return root.get("status").in(statuses);
        };
    }

    public static Specification<Order> createdBetweenAndHasStatuses(
            LocalDateTime from,
            LocalDateTime to,
            List<OrderStatus> statuses) {
        return Specification
                .where(createdBetween(from, to))
                .and(statusIn(statuses));
    }
}
