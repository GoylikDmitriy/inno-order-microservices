package com.goylik.payment_service.model.entity;

import com.goylik.payment_service.model.enums.PaymentStatus;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Document(collection = "payments")
@CompoundIndex(name = "idx_user_id_timestamp", def = "{'user_id': 1, 'timestamp': -1}")
@Getter @Setter
public class Payment {
    @Id
    private String id;

    @Field("order_id")
    @Indexed
    private Long orderId;

    @Field("user_id")
    @Indexed
    private Long userId;

    @Field("status")
    @Indexed
    private PaymentStatus status;

    @Field("timestamp")
    @Indexed(name = "idx_timestamp", direction = IndexDirection.DESCENDING)
    @CreatedDate
    private LocalDateTime timestamp;

    @Field("payment_amount")
    private BigDecimal paymentAmount;

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Payment payment = (Payment) o;
        return Objects.equals(id, payment.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "Payment{" +
                "id='" + id + '\'' +
                ", orderId=" + orderId +
                ", userId=" + userId +
                ", status=" + status +
                ", timestamp=" + timestamp +
                ", paymentAmount=" + paymentAmount +
                '}';
    }
}
