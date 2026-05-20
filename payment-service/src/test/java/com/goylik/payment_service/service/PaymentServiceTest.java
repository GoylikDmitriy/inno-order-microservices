package com.goylik.payment_service.service;

import com.goylik.payment_service.kafka.PaymentEventProducer;
import com.goylik.payment_service.mapper.PaymentMapper;
import com.goylik.payment_service.model.dto.request.CreatePaymentRequest;
import com.goylik.payment_service.model.dto.request.PaymentFilterRequest;
import com.goylik.payment_service.model.dto.response.PaymentResponse;
import com.goylik.payment_service.model.entity.Payment;
import com.goylik.payment_service.model.enums.PaymentStatus;
import com.goylik.payment_service.repository.PaymentRepository;
import com.goylik.payment_service.service.impl.PaymentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentMapper paymentMapper;
    @Mock private PaymentProcessingService paymentProcessingService;
    @Mock private PaymentEventProducer paymentEventProducer;

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Payment payment;
    private PaymentResponse paymentResponse;

    @BeforeEach
    void setUp() {
        payment = new Payment();
        payment.setId("pay-123");
        payment.setOrderId(1L);
        payment.setUserId(10L);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setPaymentAmount(BigDecimal.valueOf(99.99));
        payment.setTimestamp(LocalDateTime.now());

        paymentResponse = new PaymentResponse(
                "pay-123",
                1L,
                10L,
                PaymentStatus.SUCCESS,
                payment.getTimestamp(),
                BigDecimal.valueOf(99.99)
        );
    }

    @Test
    void createPayment_ShouldReturnPaymentResponse_WhenSuccess() {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 10L, BigDecimal.valueOf(99.99));

        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(paymentProcessingService.pay()).thenReturn(PaymentStatus.SUCCESS);
        when(paymentRepository.insert(payment)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        PaymentResponse result = paymentService.createPayment(request);

        assertNotNull(result);
        assertEquals("pay-123", result.id());
        assertEquals(PaymentStatus.SUCCESS, result.status());
        verify(paymentRepository).insert(payment);
        verify(paymentEventProducer).sendPaymentCreatedEvent(payment);
    }

    @Test
    void createPayment_ShouldSetStatusFromProcessingService() {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 10L, BigDecimal.valueOf(99.99));

        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(paymentProcessingService.pay()).thenReturn(PaymentStatus.FAILED);
        when(paymentRepository.insert(payment)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        paymentService.createPayment(request);

        assertEquals(PaymentStatus.FAILED, payment.getStatus());
    }

    @Test
    void createPayment_ShouldPublishEvent_AfterSaving() {
        CreatePaymentRequest request = new CreatePaymentRequest(1L, 10L, BigDecimal.valueOf(99.99));

        when(paymentMapper.toEntity(request)).thenReturn(payment);
        when(paymentProcessingService.pay()).thenReturn(PaymentStatus.SUCCESS);
        when(paymentRepository.insert(payment)).thenReturn(payment);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        paymentService.createPayment(request);

        InOrder inOrder = inOrder(paymentRepository, paymentEventProducer);
        inOrder.verify(paymentRepository).insert(payment);
        inOrder.verify(paymentEventProducer).sendPaymentCreatedEvent(payment);
    }

    @Test
    void getPayments_ShouldFilterByUserId_WhenUserIdProvided() {
        PaymentFilterRequest request = new PaymentFilterRequest(10L, null, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(payment));

        when(paymentRepository.findByUserId(10L, pageable)).thenReturn(page);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        Page<PaymentResponse> result = paymentService.getPayments(request, pageable);

        assertEquals(1, result.getTotalElements());
        verify(paymentRepository).findByUserId(10L, pageable);
        verify(paymentRepository, never()).findByOrderId(any(), any());
        verify(paymentRepository, never()).findByStatus(any(), any());
    }

    @Test
    void getPayments_ShouldFilterByOrderId_WhenOrderIdProvided() {
        PaymentFilterRequest request = new PaymentFilterRequest(null, 1L, null);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(payment));

        when(paymentRepository.findByOrderId(1L, pageable)).thenReturn(page);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        Page<PaymentResponse> result = paymentService.getPayments(request, pageable);

        assertEquals(1, result.getTotalElements());
        verify(paymentRepository).findByOrderId(1L, pageable);
        verify(paymentRepository, never()).findByUserId(any(), any());
        verify(paymentRepository, never()).findByStatus(any(), any());
    }

    @Test
    void getPayments_ShouldFilterByStatus_WhenStatusProvided() {
        PaymentFilterRequest request = new PaymentFilterRequest(null, null, PaymentStatus.SUCCESS);
        Pageable pageable = PageRequest.of(0, 10);
        Page<Payment> page = new PageImpl<>(List.of(payment));

        when(paymentRepository.findByStatus(PaymentStatus.SUCCESS, pageable)).thenReturn(page);
        when(paymentMapper.toResponse(payment)).thenReturn(paymentResponse);

        Page<PaymentResponse> result = paymentService.getPayments(request, pageable);

        assertEquals(1, result.getTotalElements());
        verify(paymentRepository).findByStatus(PaymentStatus.SUCCESS, pageable);
        verify(paymentRepository, never()).findByUserId(any(), any());
        verify(paymentRepository, never()).findByOrderId(any(), any());
    }

    @Test
    void getPayments_ShouldReturnEmptyPage_WhenNoPaymentsFound() {
        PaymentFilterRequest request = new PaymentFilterRequest(10L, null, null);
        Pageable pageable = PageRequest.of(0, 10);

        when(paymentRepository.findByUserId(10L, pageable)).thenReturn(Page.empty());

        Page<PaymentResponse> result = paymentService.getPayments(request, pageable);

        assertTrue(result.isEmpty());
    }
}
