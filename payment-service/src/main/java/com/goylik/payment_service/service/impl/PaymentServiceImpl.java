package com.goylik.payment_service.service.impl;

import com.goylik.payment_service.kafka.PaymentEventProducer;
import com.goylik.payment_service.mapper.PaymentMapper;
import com.goylik.payment_service.model.dto.request.CreatePaymentRequest;
import com.goylik.payment_service.model.dto.request.PaymentFilterRequest;
import com.goylik.payment_service.model.dto.response.PaymentResponse;
import com.goylik.payment_service.model.entity.Payment;
import com.goylik.payment_service.model.enums.PaymentStatus;
import com.goylik.payment_service.repository.PaymentRepository;
import com.goylik.payment_service.service.PaymentProcessingService;
import com.goylik.payment_service.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final PaymentProcessingService paymentProcessingService;
    private final PaymentEventProducer paymentEventProducer;

    @Override
    public PaymentResponse createPayment(CreatePaymentRequest request) {
        var payment = paymentMapper.toEntity(request);
        payment.setStatus(pay());

        var savedPayment = paymentRepository.insert(payment);

        paymentEventProducer.sendPaymentCreatedEvent(savedPayment);

        return paymentMapper.toResponse(savedPayment);
    }

    private PaymentStatus pay() {
        return paymentProcessingService.pay();
    }

    @Override
    public Page<PaymentResponse> getPayments(PaymentFilterRequest request, Pageable pageable) {
        Page<Payment> payments;

        if (request.userId() != null) {
            payments = paymentRepository.findByUserId(request.userId(), pageable);
        } else if (request.orderId() != null) {
            payments = paymentRepository.findByOrderId(request.orderId(), pageable);
        } else {
            payments = paymentRepository.findByStatus(request.status(), pageable);
        }

        return payments.map(paymentMapper::toResponse);
    }
}
