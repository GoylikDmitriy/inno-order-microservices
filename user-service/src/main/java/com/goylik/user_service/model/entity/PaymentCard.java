package com.goylik.user_service.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

import java.time.YearMonth;

@Entity
@Table(name = "payment_cards")
@Getter @Setter
@SQLDelete(sql = "UPDATE payment_cards SET deleted_at = now() WHERE id=?")
@SQLRestriction("deleted_at IS NULL")
public class PaymentCard extends AuditableEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "number", nullable = false, length = 255)
    private String number;

    @Column(name = "number_hash", nullable = false, unique = true)
    private String numberHash;

    @Column(name = "holder", nullable = false, length = 255)
    private String holder;

    @Column(name = "expiration_date", nullable = false)
    private YearMonth expirationDate;

    @Column(name = "active")
    private Boolean active = true;

    @Override
    public String toString() {
        return "PaymentCard{" +
                "id=" + id +
                ", holder='" + holder + '\'' +
                ", expirationDate=" + expirationDate +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PaymentCard card)) return false;
        return id != null && id.equals(card.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }
}