package com.goylik.payment_service.service;

import com.goylik.payment_service.model.dto.request.DateRangeRequest;
import com.goylik.payment_service.model.dto.response.TotalAmountResponse;
import com.goylik.payment_service.repository.PaymentRepository;
import com.goylik.payment_service.security.util.SecurityUtils;
import com.goylik.payment_service.service.impl.PaymentStatisticsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentStatisticsServiceTest {
    @Mock private PaymentRepository paymentRepository;

    @InjectMocks
    private PaymentStatisticsServiceImpl paymentStatisticsService;

    private static final Long USER_ID = 1L;
    private static final LocalDateTime FROM = LocalDateTime.of(2024, 1, 1, 0, 0);
    private static final LocalDateTime TO = LocalDateTime.of(2024, 12, 31, 23, 59);

    private DateRangeRequest dateRangeRequest;

    @BeforeEach
    void setUp() {
        dateRangeRequest = new DateRangeRequest(FROM, TO);
    }

    @Test
    void getTotalAmountForCurrentUser_ShouldReturnTotalAmount() {
        BigDecimal expected = BigDecimal.valueOf(250.00);
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);

            when(paymentRepository.getTotalAmountByUserIdAndDateRange(USER_ID, FROM, TO))
                    .thenReturn(expected);

            TotalAmountResponse result = paymentStatisticsService
                    .getTotalAmountForCurrentUser(dateRangeRequest);

            assertNotNull(result);
            assertEquals(expected, result.totalAmount());
            assertEquals(FROM, result.from());
            assertEquals(TO, result.to());
            verify(paymentRepository).getTotalAmountByUserIdAndDateRange(USER_ID, FROM, TO);
        }
    }

    @Test
    void getTotalAmountForCurrentUser_ShouldReturnZero_WhenNoPayments() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
            when(paymentRepository.getTotalAmountByUserIdAndDateRange(USER_ID, FROM, TO))
                    .thenReturn(BigDecimal.ZERO);

            TotalAmountResponse result = paymentStatisticsService
                    .getTotalAmountForCurrentUser(dateRangeRequest);

            assertEquals(BigDecimal.ZERO, result.totalAmount());
        }
    }

    @Test
    void getTotalAmountForCurrentUser_ShouldPassCorrectDateRange() {
        try (MockedStatic<SecurityUtils> mocked = mockStatic(SecurityUtils.class)) {
            mocked.when(SecurityUtils::getCurrentUserId).thenReturn(USER_ID);
            when(paymentRepository.getTotalAmountByUserIdAndDateRange(any(), any(), any()))
                    .thenReturn(BigDecimal.TEN);

            paymentStatisticsService.getTotalAmountForCurrentUser(dateRangeRequest);

            verify(paymentRepository).getTotalAmountByUserIdAndDateRange(USER_ID, FROM, TO);
        }
    }
    @Test
    void getTotalAmountForAllUsers_ShouldReturnTotalAmount() {
        BigDecimal expected = BigDecimal.valueOf(1500.00);
        when(paymentRepository.getTotalAmountByDateRange(FROM, TO))
                .thenReturn(expected);

        TotalAmountResponse result = paymentStatisticsService
                .getTotalAmountForAllUsers(dateRangeRequest);

        assertNotNull(result);
        assertEquals(expected, result.totalAmount());
        assertEquals(FROM, result.from());
        assertEquals(TO, result.to());
        verify(paymentRepository).getTotalAmountByDateRange(FROM, TO);
    }

    @Test
    void getTotalAmountForAllUsers_ShouldReturnZero_WhenNoPayments() {
        when(paymentRepository.getTotalAmountByDateRange(FROM, TO))
                .thenReturn(BigDecimal.ZERO);

        TotalAmountResponse result = paymentStatisticsService
                .getTotalAmountForAllUsers(dateRangeRequest);

        assertEquals(BigDecimal.ZERO, result.totalAmount());
    }

    @Test
    void getTotalAmountForAllUsers_ShouldPassCorrectDateRange() {
        when(paymentRepository.getTotalAmountByDateRange(any(), any()))
                .thenReturn(BigDecimal.TEN);

        paymentStatisticsService.getTotalAmountForAllUsers(dateRangeRequest);

        verify(paymentRepository).getTotalAmountByDateRange(FROM, TO);
    }
}
