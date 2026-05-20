package com.goylik.user_service.util.scheduler;

import com.goylik.user_service.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;

@Service
@RequiredArgsConstructor
@Slf4j
public class CardExpiryService {
    private final PaymentCardRepository cardRepository;

    @Transactional
    public void deactivateExpiredCards() {
        int updated = cardRepository.deactivateExpiredCards(YearMonth.now());
        log.info("{} cards were expired and had been deactivated in this month.", updated);
    }
}