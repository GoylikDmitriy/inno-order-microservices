package com.goylik.payment_service.service;

import com.goylik.payment_service.model.dto.request.CreatePaymentRequest;
import com.goylik.payment_service.model.dto.request.PaymentFilterRequest;
import com.goylik.payment_service.model.dto.response.PaymentResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Service for managing payments.
 */
public interface PaymentService {
    /**
     * Creates a new payment based on the provided request.
     *
     * @param request the request object containing payment data.
     * @return a response with information about the created payment.
     */
    PaymentResponse createPayment(CreatePaymentRequest request);

    /**
     * Returns a paginated list of payments filtered by the given criteria.
     *
     * @param request   the filter request containing filter conditions
     * @param pageable  pagination information
     * @return a page of payment responses matching the filter criteria
     */
    Page<PaymentResponse> getPayments(PaymentFilterRequest request, Pageable pageable);
}
