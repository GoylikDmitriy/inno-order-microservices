package com.goylik.payment_service.service;

import com.goylik.payment_service.model.enums.PaymentStatus;
import com.goylik.payment_service.service.impl.PaymentProcessingServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentProcessingServiceTest {
    @Mock private RestClient restClient;
    @Mock private RestClient.RequestHeadersUriSpec requestHeadersUriSpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    @InjectMocks
    private PaymentProcessingServiceImpl paymentProcessingService;

    @Test
    void pay_ShouldReturnSuccess_WhenRandomNumberIsEven() {
        mockRestClient("42");

        PaymentStatus result = paymentProcessingService.pay();

        assertEquals(PaymentStatus.SUCCESS, result);
    }

    @ParameterizedTest
    @CsvSource({
            "13, FAILED",
            "not-a-number, FAILED",
            "null, FAILED",
            "1, FAILED"
    })
    void pay_ShouldReturnFailed_ForVariousInvalidOrOddResponses(String input, PaymentStatus expected) {
        mockRestClient(input);
        PaymentStatus result = paymentProcessingService.pay();
        assertEquals(expected, result);
    }

    @Test
    void pay_ShouldReturnFailed_WhenRestClientThrowsException() {
        when(restClient.get()).thenThrow(new RuntimeException("Connection refused"));

        PaymentStatus result = paymentProcessingService.pay();

        assertEquals(PaymentStatus.FAILED, result);
    }

    @Test
    void pay_ShouldReturnSuccess_WhenBoundaryEvenNumber() {
        mockRestClient("100");

        PaymentStatus result = paymentProcessingService.pay();

        assertEquals(PaymentStatus.SUCCESS, result);
    }

    private void mockRestClient(String response) {
        when(restClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri(anyString())).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(String.class)).thenReturn(response);
    }
}
