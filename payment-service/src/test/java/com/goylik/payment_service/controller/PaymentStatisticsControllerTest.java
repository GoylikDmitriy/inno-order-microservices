package com.goylik.payment_service.controller;

import com.goylik.payment_service.model.entity.Payment;
import com.goylik.payment_service.model.enums.PaymentStatus;
import com.goylik.payment_service.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentStatisticsControllerTest extends BaseIntegrationTest {
    @Autowired
    private PaymentRepository paymentRepository;

    private static final String BASE_URL = "/api/payments/stats";
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void getTotalForCurrentUser_ShouldReturn200_WhenUserRequests() throws Exception {
        createPayment(USER_ID, PaymentStatus.SUCCESS, BigDecimal.valueOf(100.00));
        createPayment(USER_ID, PaymentStatus.SUCCESS, BigDecimal.valueOf(50.00));
        createPayment(USER_ID, PaymentStatus.FAILED, BigDecimal.valueOf(30.00));

        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/total/me")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-12-31T23:59:59")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(150.00))
                .andExpect(jsonPath("$.from").isNotEmpty())
                .andExpect(jsonPath("$.to").isNotEmpty());
    }

    @Test
    void getTotalForCurrentUser_ShouldReturn200_WhenAdminRequests() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/total/me")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-12-31T23:59:59")))
                .andExpect(status().isOk());
    }

    @Test
    void getTotalForCurrentUser_ShouldReturnZero_WhenNoPaymentsInRange() throws Exception {
        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/total/me")
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2020-12-31T23:59:59")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    @Test
    void getTotalForCurrentUser_ShouldReturn400_WhenFromAfterTo() throws Exception {
        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/total/me")
                        .param("from", "2026-12-31T23:59:59")
                        .param("to", "2026-01-01T00:00:00")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTotalForCurrentUser_ShouldReturn400_WhenMissingParams() throws Exception {
        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/total/me")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getTotalForAllUsers_ShouldReturn200_WhenAdminRequests() throws Exception {
        createPayment(USER_ID, PaymentStatus.SUCCESS, BigDecimal.valueOf(100.00));
        createPayment(999L, PaymentStatus.SUCCESS, BigDecimal.valueOf(200.00));

        mockMvc.perform(withAdmin(get(BASE_URL + "/total")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-12-31T23:59:59")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(300.00));
    }

    @Test
    void getTotalForAllUsers_ShouldReturn403_WhenUserRequests() throws Exception {
        mockMvc.perform(withUser(USER_ID, get(BASE_URL + "/total")
                        .param("from", "2026-01-01T00:00:00")
                        .param("to", "2026-12-31T23:59:59")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getTotalForAllUsers_ShouldReturnZero_WhenNoPayments() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/total")
                        .param("from", "2020-01-01T00:00:00")
                        .param("to", "2020-12-31T23:59:59")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAmount").value(0));
    }

    private void createPayment(Long userId, PaymentStatus status, BigDecimal amount) {
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setOrderId(1L);
        payment.setStatus(status);
        payment.setPaymentAmount(amount);
        paymentRepository.insert(payment);
    }
}
