package com.goylik.payment_service.service.impl;

import com.goylik.payment_service.model.dto.request.DateRangeRequest;
import com.goylik.payment_service.model.dto.response.TotalAmountResponse;
import com.goylik.payment_service.repository.PaymentRepository;
import com.goylik.payment_service.security.util.SecurityUtils;
import com.goylik.payment_service.service.PaymentStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentStatisticsServiceImpl implements PaymentStatisticsService {
    private final PaymentRepository paymentRepository;

    @Override
    public TotalAmountResponse getTotalAmountForCurrentUser(DateRangeRequest request) {
        Long userId = SecurityUtils.getCurrentUserId();
        var totalAmount = paymentRepository
                .getTotalAmountByUserIdAndDateRange(userId, request.from(), request.to());

        return new TotalAmountResponse(totalAmount, request.from(), request.to());
    }

    @Override
    public TotalAmountResponse getTotalAmountForAllUsers(DateRangeRequest request) {
        var totalAmount = paymentRepository
                .getTotalAmountByDateRange(request.from(), request.to());

        return new TotalAmountResponse(totalAmount, request.from(), request.to());
    }
}
