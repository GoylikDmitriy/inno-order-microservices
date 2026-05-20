package com.goylik.order_service.specification;

import com.goylik.order_service.model.entity.Order;
import com.goylik.order_service.model.enums.OrderStatus;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderSpecificationTest {
    @Mock private Root<Order> root;
    @Mock private CriteriaBuilder cb;
    @Mock private Predicate mockPredicate;


    @Test
    void createdBetweenAndHasStatuses_ShouldBuildSpecification() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        List<OrderStatus> statuses = List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);

        Specification<Order> spec = OrderSpecification
                .createdBetweenAndHasStatuses(from, to, statuses);

        assertThat(spec).isNotNull();
    }

    @Test
    void createdBetween_ShouldBuildSpecification() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();

        when(cb.between(any(), eq(from), eq(to))).thenReturn(mockPredicate);

        Specification<Order> spec = OrderSpecification
                .createdBetween(from, to);

        assertThat(spec).isNotNull();

        Predicate result = spec.toPredicate(root, null, cb);

        assertThat(result).isEqualTo(mockPredicate);
        verify(cb).between(any(), eq(from), eq(to));
    }

    @Test
    void createdBetween_ShouldBuildSpecification_WhenFromDateIsNull() {
        LocalDateTime from = null;
        LocalDateTime to = LocalDateTime.now();

        when(cb.lessThanOrEqualTo(any(), eq(to))).thenReturn(mockPredicate);

        Specification<Order> spec = OrderSpecification
                .createdBetween(from, to);

        assertThat(spec).isNotNull();

        Predicate result = spec.toPredicate(root, null, cb);

        assertThat(result).isEqualTo(mockPredicate);
        verify(cb).lessThanOrEqualTo(any(), eq(to));
    }

    @Test
    void createdBetween_ShouldBuildSpecification_WhenToDateIsNull() {
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = null;

        when(cb.greaterThanOrEqualTo(any(), eq(from))).thenReturn(mockPredicate);

        Specification<Order> spec = OrderSpecification
                .createdBetween(from, to);

        assertThat(spec).isNotNull();

        Predicate result = spec.toPredicate(root, null, cb);

        assertThat(result).isEqualTo(mockPredicate);
        verify(cb).greaterThanOrEqualTo(any(), eq(from));
    }

    @Test
    void createdBetween_ShouldReturnConjunction_WhenBothDatesAreNull() {
        LocalDateTime from = null;
        LocalDateTime to = null;

        when(cb.conjunction()).thenReturn(mockPredicate);

        Specification<Order> spec = OrderSpecification
                .createdBetween(from, to);

        Predicate result = spec.toPredicate(root, null, cb);

        assertThat(result).isEqualTo(mockPredicate);
        verify(cb).conjunction();
    }

    @Test
    void statusIn_ShouldBuildSpecification() {
        List<OrderStatus> statuses = List.of(OrderStatus.PENDING, OrderStatus.CONFIRMED);

        Specification<Order> spec = OrderSpecification
                .statusIn(statuses);

        assertThat(spec).isNotNull();
    }
}
