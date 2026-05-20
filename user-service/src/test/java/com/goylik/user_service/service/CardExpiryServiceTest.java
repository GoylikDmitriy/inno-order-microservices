package com.goylik.user_service.service;

import com.goylik.user_service.repository.PaymentCardRepository;
import com.goylik.user_service.util.scheduler.CardExpiryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.YearMonth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CardExpiryServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;

    @InjectMocks
    private CardExpiryService cardExpiryService;

    @Test
    void deactivateExpiredCards_shouldCallRepository() {
        when(cardRepository.deactivateExpiredCards(any(YearMonth.class))).thenReturn(3);

        cardExpiryService.deactivateExpiredCards();

        verify(cardRepository).deactivateExpiredCards(YearMonth.now());
    }

    @Test
    void deactivateExpiredCards_shouldWorkWhenNoExpiredCards() {
        when(cardRepository.deactivateExpiredCards(any(YearMonth.class))).thenReturn(0);

        cardExpiryService.deactivateExpiredCards();

        verify(cardRepository).deactivateExpiredCards(YearMonth.now());
    }
}
