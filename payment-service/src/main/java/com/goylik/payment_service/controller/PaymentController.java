package com.goylik.payment_service.controller;

import com.goylik.payment_service.model.dto.request.CreatePaymentRequest;
import com.goylik.payment_service.model.dto.request.PaymentFilterRequest;
import com.goylik.payment_service.model.dto.response.PaymentResponse;
import com.goylik.payment_service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Validated
public class PaymentController {
    private final PaymentService paymentService;

    /**
     * Creates a new payment.
     * <p>Only ADMIN users can create payments for any user.
     * A USER can create a payment only for themselves.</p>
     *
     * @param request the payment creation request (validated)
     * @return the created payment response
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or #request.userId() == authentication.principal.userId")
    public PaymentResponse createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        return paymentService.createPayment(request);
    }

    /**
     * Retrieves a paginated list of payments based on the provided filters.
     * <p>ADMIN users can view payments of any user.
     * Regular users can only view their own payments.</p>
     *
     * @param request  the filter request
     * @param pageable pagination parameters
     * @return a page of payment responses matching the filter criteria
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or " +
            "(#request.userId() != null and #request.userId() == authentication.principal.userId)")
    public Page<PaymentResponse> getPayments(@ModelAttribute PaymentFilterRequest request, Pageable pageable) {
        return paymentService.getPayments(request, pageable);
    }
}
