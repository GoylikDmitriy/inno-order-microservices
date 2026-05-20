package com.goylik.user_service.util.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class CardExpiryScheduler {
    private final CardExpiryService cardExpiryService;

    @Scheduled(cron = "0 0 0 1 * ?")
    public void runMonthlyExpiryCheck() {
        log.info("Running monthly card expiry scheduler...");
        cardExpiryService.deactivateExpiredCards();
    }
}