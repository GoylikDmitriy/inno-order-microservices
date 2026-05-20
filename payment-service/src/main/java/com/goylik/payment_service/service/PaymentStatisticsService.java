package com.goylik.payment_service.service;

import com.goylik.payment_service.model.dto.response.TotalAmountResponse;
import com.goylik.payment_service.model.dto.request.DateRangeRequest;

/**
 * Service for payment statistics.
 */
public interface PaymentStatisticsService {
    /**
     * Returns the total amount of payments made by a specific user within a date range.
     *
     * @param request the date range request (start and end dates)
     * @return a response containing the total amount for the given user
     */
    TotalAmountResponse getTotalAmountForCurrentUser(DateRangeRequest request);

    /**
     * Returns the total amount of payments made by all users within a date range.
     *
     * @param request the date range request (start and end dates)
     * @return a response containing the total amount across all users
     */
    TotalAmountResponse getTotalAmountForAllUsers(DateRangeRequest request);
}
