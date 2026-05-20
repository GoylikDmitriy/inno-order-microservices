package com.goylik.order_service.model.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@Getter @Setter
@EntityListeners(AuditingEntityListener.class)
public abstract class AuditableEntity {
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    protected LocalDateTime createdAt;

    @Column(name = "updated_at")
    @LastModifiedDate
    protected LocalDateTime updatedAt;
}
