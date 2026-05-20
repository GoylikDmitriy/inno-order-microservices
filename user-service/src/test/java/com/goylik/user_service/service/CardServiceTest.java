package com.goylik.user_service.service;

import com.goylik.user_service.exception.card.*;
import com.goylik.user_service.mapper.CardMapper;
import com.goylik.user_service.model.dto.request.CreateCardRequest;
import com.goylik.user_service.model.dto.request.UpdateCardRequest;
import com.goylik.user_service.model.dto.response.CardResponse;
import com.goylik.user_service.model.entity.PaymentCard;
import com.goylik.user_service.model.entity.User;
import com.goylik.user_service.repository.PaymentCardRepository;
import com.goylik.user_service.repository.UserRepository;
import com.goylik.user_service.service.impl.CardServiceImpl;
import com.goylik.user_service.util.CardNumberUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardServiceTest {

    @Mock
    private PaymentCardRepository cardRepository;

    @Mock
    private CardCryptoService cardCryptoService;

    @Mock
    private CardHashService cardHashService;

    @Mock
    private CacheManager cacheManager;

    @Mock
    private CardMapper cardMapper;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CardServiceImpl cardService;

    private PaymentCard card;
    private CardResponse response;

    @BeforeEach
    void setUp() {
        User user = new User();
        user.setId(10L);
        user.setName("John");
        user.setSurname("Doe");
        user.setEmail("john@mail.com");
        user.setBirthDate(LocalDate.of(2000, 11, 5));
        user.setActive(true);

        card = new PaymentCard();
        card.setId(1L);
        card.setUser(user);
        card.setNumber("encrypted");
        card.setHolder("John Doe");
        card.setExpirationDate(YearMonth.of(2029, 10));
        card.setActive(true);

        response = new CardResponse(
                1L,
                10L,
                "1111222233334444",
                "John Doe",
                YearMonth.of(2029, 10),
                true
        );

        ReflectionTestUtils.setField(cardService, "cardLimitPerUser", 5);
    }

    @Test
    void createCard_shouldCreateCardSuccessfully() {
        CreateCardRequest request = new CreateCardRequest(
                10L,
                "1111222233334444",
                "John Doe",
                YearMonth.of(2029, 10)
        );

        try (MockedStatic<CardNumberUtils> mocked = mockStatic(CardNumberUtils.class)) {
            mocked.when(() -> CardNumberUtils.validate(request.number())).thenReturn(true);

            when(userRepository.findByIdWithLock(request.userId())).thenReturn(Optional.of(card.getUser()));
            when(cardRepository.countByUserId(10L)).thenReturn(1L);
            when(cardMapper.toEntity(request)).thenReturn(card);
            when(cardCryptoService.encrypt(request.number())).thenReturn("encrypted");
            when(cardHashService.hash(request.number())).thenReturn("12345");
            when(cardRepository.save(card)).thenReturn(card);
            when(cardCryptoService.decrypt("encrypted")).thenReturn("1111222233334444");
            when(cardMapper.toResponse(card, "1111222233334444")).thenReturn(response);

            CardResponse result = cardService.createCard(request);

            assertNotNull(result);

            verify(cardRepository).save(card);
            verify(cardCryptoService).encrypt(request.number());
        }
    }

    @Test
    void createCard_shouldThrowLimitException() {
        CreateCardRequest request = new CreateCardRequest(
                10L,
                "1111222233334444",
                "John Doe",
                YearMonth.of(2029, 10)
        );

        when(userRepository.findByIdWithLock(10L)).thenReturn(Optional.of(card.getUser()));
        when(cardRepository.countByUserId(10L)).thenReturn(5L);

        assertThrows(
                CardLimitExceededException.class,
                () -> cardService.createCard(request)
        );
    }

    @Test
    void createCard_shouldThrowInvalidNumber() {
        CreateCardRequest request = new CreateCardRequest(
                10L,
                "1111222233334444",
                "John Doe",
                YearMonth.of(2029, 10)
        );

        try (MockedStatic<CardNumberUtils> mocked = mockStatic(CardNumberUtils.class)) {
            mocked.when(() -> CardNumberUtils.validate(request.number())).thenReturn(false);

            when(userRepository.findByIdWithLock(10L)).thenReturn(Optional.of(card.getUser()));
            when(cardRepository.countByUserId(10L)).thenReturn(1L);

            assertThrows(
                    InvalidCardNumberException.class,
                    () -> cardService.createCard(request)
            );
        }
    }

    @Test
    void createCard_shouldThrowCardCryptoException() {
        CreateCardRequest request = new CreateCardRequest(
                10L, "1111222233334444", "John Doe", YearMonth.of(2029, 10)
        );

        try (MockedStatic<CardNumberUtils> mocked = mockStatic(CardNumberUtils.class)) {
            mocked.when(() -> CardNumberUtils.validate(request.number())).thenReturn(true);

            when(userRepository.findByIdWithLock(10L)).thenReturn(Optional.of(card.getUser()));
            when(cardRepository.countByUserId(10L)).thenReturn(1L);
            when(cardMapper.toEntity(request)).thenReturn(card);
            when(cardCryptoService.encrypt(request.number()))
                    .thenThrow(new CardCryptoException("Encryption failed"));

            assertThrows(
                    CardCryptoException.class,
                    () -> cardService.createCard(request)
            );
        }
    }

    @Test
    void createCard_shouldThrowCardHashingException() {
        CreateCardRequest request = new CreateCardRequest(
                10L, "1111222233334444", "John Doe", YearMonth.of(2029, 10)
        );

        try (MockedStatic<CardNumberUtils> mocked = mockStatic(CardNumberUtils.class)) {
            mocked.when(() -> CardNumberUtils.validate(request.number())).thenReturn(true);

            when(userRepository.findByIdWithLock(10L)).thenReturn(Optional.of(card.getUser()));
            when(cardRepository.countByUserId(10L)).thenReturn(1L);
            when(cardMapper.toEntity(request)).thenReturn(card);
            when(cardCryptoService.encrypt(request.number())).thenReturn("encrypted");
            when(cardHashService.hash(request.number()))
                    .thenThrow(new CardHashingException("Hashing failed"));

            assertThrows(
                    CardHashingException.class,
                    () -> cardService.createCard(request)
            );
        }
    }

    @Test
    void getCardById_shouldReturnCard() {
        when(cardRepository.findByIdWithUser(1L)).thenReturn(Optional.of(card));
        when(cardCryptoService.decrypt("encrypted")).thenReturn("1111222233334444");
        when(cardMapper.toResponse(card, "1111222233334444")).thenReturn(response);

        CardResponse result = cardService.getCardById(1L);

        assertEquals(1L, result.id());

        verify(cardRepository).findByIdWithUser(1L);
    }

    @Test
    void getCardById_shouldThrowExceptionWhenCardNotFound() {
        when(cardRepository.findByIdWithUser(1L)).thenReturn(Optional.empty());

        assertThrows(
                CardNotFoundException.class,
                () -> cardService.getCardById(1L)
        );
    }

    @Test
    void getAll_shouldReturnPageOfCards() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaymentCard> page = new PageImpl<>(List.of(card));

        when(cardRepository.findAllWithUser(pageable)).thenReturn(page);
        when(cardCryptoService.decrypt("encrypted")).thenReturn("1111222233334444");
        when(cardMapper.toResponse(card, "1111222233334444")).thenReturn(response);

        Page<CardResponse> result = cardService.getAll(pageable);

        assertEquals(1, result.getTotalElements());
    }

    @Test
    void getAllCardsByUserId_shouldReturnListOfUsersCards() {
        when(cardRepository.findByUserIdWithUser(10L)).thenReturn(List.of(card));
        when(cardCryptoService.decrypt("encrypted")).thenReturn("1111222233334444");
        when(cardMapper.toResponse(card, "1111222233334444")).thenReturn(response);

        List<CardResponse> result = cardService.getAllCardsByUserId(10L);

        assertEquals(1, result.size());
    }

    @Test
    void updateCard_shouldUpdateCardWithoutChangingNumber() {
        UpdateCardRequest request = new UpdateCardRequest(null, "New Holder", null);

        when(cardRepository.findByIdWithUser(1L)).thenReturn(Optional.of(card));
        when(cardRepository.save(card)).thenReturn(card);
        when(cardCryptoService.decrypt("encrypted")).thenReturn("1111222233334444");
        when(cardMapper.toResponse(card, "1111222233334444")).thenReturn(response);

        CardResponse result = cardService.updateCard(1L, request);

        assertNotNull(result);

        verify(cardRepository).save(card);
    }

    @Test
    void updateCard_shouldUpdateCardWithChangingNumber() {
        UpdateCardRequest request = new UpdateCardRequest("1111222233334444", null, null);

        try (MockedStatic<CardNumberUtils> mocked = mockStatic(CardNumberUtils.class)) {
            mocked.when(() -> CardNumberUtils.validate(request.number())).thenReturn(true);

            when(cardRepository.findByIdWithUser(1L)).thenReturn(Optional.of(card));
            when(cardCryptoService.encrypt(request.number())).thenReturn("encrypted");
            when(cardRepository.save(card)).thenReturn(card);
            when(cardCryptoService.decrypt("encrypted")).thenReturn("1111222233334444");
            when(cardHashService.hash(request.number())).thenReturn("12345");
            when(cardMapper.toResponse(card, "1111222233334444")).thenReturn(response);

            cardService.updateCard(1L, request);

            verify(cardCryptoService).encrypt(request.number());
        }
    }

    @Test
    void updateCard_shouldThrowExceptionWhenCardNotFound() {
        when(cardRepository.findByIdWithUser(1L)).thenReturn(Optional.empty());
        UpdateCardRequest request = new UpdateCardRequest(null, "New Holder", null);

        assertThrows(
                CardNotFoundException.class,
                () -> cardService.updateCard(1L, request)
        );
    }

    @Test
    void deleteCard_shouldDeleteCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.deleteCard(1L);

        verify(cardRepository).delete(card);
    }

    @Test
    void deleteCard_shouldThrowExceptionWhenCardNotFound() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                CardNotFoundException.class,
                () -> cardService.deleteCard(1L)
        );
    }

    @Test
    void activateCard_shouldActivateCard() {
        card.setActive(false);

        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.activateCard(1L);

        assertTrue(card.getActive());
    }

    @Test
    void deactivateCard_shouldDeactivateCard() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card));

        cardService.deactivateCard(1L);

        assertFalse(card.getActive());
    }
}