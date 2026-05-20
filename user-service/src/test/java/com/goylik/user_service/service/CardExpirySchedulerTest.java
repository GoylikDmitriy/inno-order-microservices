package com.goylik.user_service.service;

import com.goylik.user_service.util.scheduler.CardExpiryScheduler;
import com.goylik.user_service.util.scheduler.CardExpiryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CardExpirySchedulerTest {

    @Mock
    private CardExpiryService cardExpiryService;

    @InjectMocks
    private CardExpiryScheduler cardExpiryScheduler;

    @Test
    void runMonthlyExpiryCheck_shouldCallCardExpiryService() {
        cardExpiryScheduler.runMonthlyExpiryCheck();

        verify(cardExpiryService).deactivateExpiredCards();
    }
}
