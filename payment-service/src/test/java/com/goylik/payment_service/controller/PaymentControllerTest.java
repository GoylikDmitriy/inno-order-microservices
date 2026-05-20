package com.goylik.payment_service.controller;

import com.goylik.payment_service.kafka.PaymentEventProducer;
import com.goylik.payment_service.model.entity.Payment;
import com.goylik.payment_service.model.enums.PaymentStatus;
import com.goylik.payment_service.repository.PaymentRepository;
import com.goylik.payment_service.service.PaymentProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentControllerTest extends BaseIntegrationTest {
    @Autowired
    private PaymentRepository paymentRepository;

    @MockitoBean
    private PaymentProcessingService paymentProcessingService;

    @MockitoBean
    private PaymentEventProducer paymentEventProducer;

    private static final String BASE_URL = "/api/payments";
    private static final Long USER_ID = 10L;

    @BeforeEach
    void setUp() {
        paymentRepository.deleteAll();
    }

    @Test
    void createPayment_ShouldReturn201_WhenValidRequest() throws Exception {
        when(paymentProcessingService.pay()).thenReturn(PaymentStatus.SUCCESS);

        String request = """
                {
                    "orderId": 1,
                    "userId": %d,
                    "paymentAmount": 99.99
                }
                """.formatted(USER_ID);

        mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.orderId").value(1))
                .andExpect(jsonPath("$.userId").value(USER_ID))
                .andExpect(jsonPath("$.status").value("SUCCESS"))
                .andExpect(jsonPath("$.paymentAmount").value(99.99));

        verify(paymentEventProducer).sendPaymentCreatedEvent(any());
    }

    @Test
    void createPayment_ShouldReturn201_WhenPaymentFails() throws Exception {
        when(paymentProcessingService.pay()).thenReturn(PaymentStatus.FAILED);

        String request = """
                {
                    "orderId": 1,
                    "userId": %d,
                    "paymentAmount": 99.99
                }
                """.formatted(USER_ID);

        mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void createPayment_ShouldReturn403_WhenUserCreatesForOtherUser() throws Exception {
        String request = """
                {
                    "orderId": 1,
                    "userId": 999,
                    "paymentAmount": 99.99
                }
                """;

        mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createPayment_ShouldReturn201_WhenAdminCreatesForAnyUser() throws Exception {
        when(paymentProcessingService.pay()).thenReturn(PaymentStatus.SUCCESS);

        String request = """
                {
                    "orderId": 1,
                    "userId": %d,
                    "paymentAmount": 99.99
                }
                """.formatted(USER_ID);

        mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated());
    }

    @Test
    void createPayment_ShouldReturn400_WhenAmountIsNegative() throws Exception {
        String request = """
                {
                    "orderId": 1,
                    "userId": %d,
                    "paymentAmount": -10.00
                }
                """.formatted(USER_ID);

        mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createPayment_ShouldReturn400_WhenFieldsMissing() throws Exception {
        String request = """
                {
                    "paymentAmount": 99.99
                }
                """;

        mockMvc.perform(withUser(USER_ID, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getPayments_ShouldReturn200_WhenFilterByOwnUserId() throws Exception {
        createPayment(USER_ID, 1L, PaymentStatus.SUCCESS);
        createPayment(USER_ID, 2L, PaymentStatus.FAILED);

        mockMvc.perform(withUser(USER_ID, get(BASE_URL)
                        .param("userId", String.valueOf(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].userId").value(USER_ID));
    }

    @Test
    void getPayments_ShouldReturn403_WhenUserFiltersOtherUserId() throws Exception {
        mockMvc.perform(withUser(USER_ID, get(BASE_URL)
                        .param("userId", "999")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getPayments_ShouldReturn200_WhenAdminFiltersAnyUserId() throws Exception {
        createPayment(USER_ID, 1L, PaymentStatus.SUCCESS);

        mockMvc.perform(withAdmin(get(BASE_URL)
                        .param("userId", String.valueOf(USER_ID))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getPayments_ShouldReturn200_WhenFilterByOrderId() throws Exception {
        createPayment(USER_ID, 1L, PaymentStatus.SUCCESS);

        mockMvc.perform(withAdmin(get(BASE_URL)
                        .param("orderId", "1")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getPayments_ShouldReturn200_WhenFilterByStatus() throws Exception {
        createPayment(USER_ID, 1L, PaymentStatus.SUCCESS);
        createPayment(USER_ID, 2L, PaymentStatus.FAILED);

        mockMvc.perform(withAdmin(get(BASE_URL)
                        .param("status", "SUCCESS")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].status").value("SUCCESS"));
    }

    @Test
    void getPayments_ShouldReturn400_WhenFilterWithTwoParams() throws Exception {
        createPayment(USER_ID, 1L, PaymentStatus.SUCCESS);
        createPayment(USER_ID, 2L, PaymentStatus.FAILED);

        mockMvc.perform(withAdmin(get(BASE_URL)
                        .param("status", "SUCCESS"))
                        .param("userId", "1"))
                .andExpect(status().isBadRequest());
    }

    private void createPayment(Long userId, Long orderId, PaymentStatus status) {
        Payment payment = new Payment();
        payment.setUserId(userId);
        payment.setOrderId(orderId);
        payment.setStatus(status);
        payment.setPaymentAmount(BigDecimal.valueOf(99.99));
        payment.setTimestamp(LocalDateTime.now());
        paymentRepository.insert(payment);
    }
}
