package com.goylik.payment_service.controller;

import com.goylik.payment_service.model.dto.request.DateRangeRequest;
import com.goylik.payment_service.model.dto.response.TotalAmountResponse;
import com.goylik.payment_service.service.PaymentStatisticsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments/stats")
@RequiredArgsConstructor
@Validated
public class PaymentStatisticsController {
    private final PaymentStatisticsService paymentService;

    /**
     * Returns the total payment amount for the currently authenticated user within a given date range.
     * <p>Accessible by both USER and ADMIN roles.</p>
     *
     * @param request the date range request (start and end dates)
     * @return the total amount response for the current user
     */
    @GetMapping("/total/me")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public TotalAmountResponse getTotalForCurrentUser(@ModelAttribute @Valid DateRangeRequest request) {
        return paymentService.getTotalAmountForCurrentUser(request);
    }

    /**
     * Returns the total payment amount for all users within a given date range.
     * <p>Restricted to ADMIN users only.</p>
     *
     * @param request the date range request (start and end dates)
     * @return the total amount response across all users
     */
    @GetMapping("/total")
    @PreAuthorize("hasRole('ADMIN')")
    public TotalAmountResponse getTotalForAllUsers(@ModelAttribute @Valid DateRangeRequest request) {
        return paymentService.getTotalAmountForAllUsers(request);
    }
}
