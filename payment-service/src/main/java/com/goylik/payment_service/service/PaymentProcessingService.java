package com.goylik.payment_service.service;

import com.goylik.payment_service.model.enums.PaymentStatus;

/**
 * Service for basic payment processing operations.
 */
public interface PaymentProcessingService {
    /**
     * Executes a payment and returns its status.
     *
     * @return the status of the payment
     */
    PaymentStatus pay();
}
