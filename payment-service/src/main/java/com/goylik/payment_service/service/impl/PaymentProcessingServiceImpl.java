package com.goylik.payment_service.service.impl;

import com.goylik.payment_service.model.enums.PaymentStatus;
import com.goylik.payment_service.service.PaymentProcessingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingServiceImpl implements PaymentProcessingService {
    private static final String RANDOM_API_URL =
            "https://www.random.org/integers/?num=1&min=1&max=100&col=1&base=10&format=plain&rnd=new";

    private final RestClient restClient;

    @Override
    public PaymentStatus pay() {
        try {
            String response = restClient.get()
                    .uri(RANDOM_API_URL)
                    .retrieve()
                    .body(String.class);

            int num = Integer.parseInt(response.trim());
            log.info("Random number generated: {}", num);

            return num % 2 == 0 ? PaymentStatus.SUCCESS : PaymentStatus.FAILED;
        } catch (Exception e) {
            log.error("Failed to get random number from random.org. Error: {}", e.getMessage());
            return PaymentStatus.FAILED;
        }
    }
}
