package com.goylik.payment_service.repository;

import com.goylik.payment_service.model.entity.Payment;
import com.goylik.payment_service.model.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PaymentRepository extends MongoRepository<Payment, String>, PaymentStatisticsRepository {
    Page<Payment> findByUserId(Long userId, Pageable pageable);
    Page<Payment> findByOrderId(Long orderId, Pageable pageable);
    Page<Payment> findByStatus(PaymentStatus status, Pageable pageable);
}