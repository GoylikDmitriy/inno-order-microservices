package com.goylik.order_service.model.entity;

import com.goylik.order_service.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "orders")
@Getter @Setter
@SQLDelete(sql = "UPDATE orders SET deleted = true WHERE id=?")
@SQLRestriction("deleted = false")
public class Order extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private OrderStatus status;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice = BigDecimal.ZERO;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Column(name = "deleted", nullable = false)
    private Boolean deleted = false;

    public static Order create(Long userId) {
        var order = new Order();
        order.setUserId(userId);
        order.setStatus(OrderStatus.PENDING);
        order.setDeleted(false);
        return order;
    }

    public void setItems(List<OrderItem> newItems) {
        items.clear();
        newItems.forEach(item -> item.setOrder(this));
        items.addAll(newItems);
        recalculateTotalPrice();
    }

    private void recalculateTotalPrice() {
        this.totalPrice = items.stream()
                .map(i -> i.getItem().getPrice()
                        .multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", userId=" + userId +
                ", status=" + status +
                ", totalPrice=" + totalPrice +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Order order = (Order) o;
        return Objects.equals(id, order.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}
