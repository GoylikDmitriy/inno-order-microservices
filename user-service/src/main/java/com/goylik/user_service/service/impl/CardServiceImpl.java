package com.goylik.user_service.service.impl;

import com.goylik.user_service.exception.card.CardLimitExceededException;
import com.goylik.user_service.exception.card.CardNotFoundException;
import com.goylik.user_service.exception.card.InvalidCardNumberException;
import com.goylik.user_service.exception.user.UserNotFoundException;
import com.goylik.user_service.mapper.CardMapper;
import com.goylik.user_service.model.dto.request.CreateCardRequest;
import com.goylik.user_service.model.dto.request.UpdateCardRequest;
import com.goylik.user_service.model.dto.response.CardResponse;
import com.goylik.user_service.model.entity.PaymentCard;
import com.goylik.user_service.model.entity.User;
import com.goylik.user_service.repository.PaymentCardRepository;
import com.goylik.user_service.repository.UserRepository;
import com.goylik.user_service.service.CardCryptoService;
import com.goylik.user_service.service.CardHashService;
import com.goylik.user_service.service.CardService;
import com.goylik.user_service.util.CardNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardServiceImpl implements CardService {
    private final PaymentCardRepository cardRepository;
    private final CardCryptoService cardCryptoService;
    private final CardHashService cardHashService;
    private final CardMapper cardMapper;
    private final UserRepository userRepository;

    private final CacheManager cacheManager;

    @Value(value = "${app.payment-card.limit-per-user:5}")
    private int cardLimitPerUser;

    @Override
    @Transactional
    @CachePut(value = "cards", key = "#result.id")
    @CacheEvict(value = "userCards", key = "#request.userId()")
    public CardResponse createCard(CreateCardRequest request) {
        var user = fetchUserByIdWithLockOrThrow(request.userId());

        validateUserCardLimitOrThrow(request.userId());
        validateCardNumberOrThrow(request.number());

        var card = mapToEntity(request, user);

        var savedCard = cardRepository.save(card);
        return decryptCardNumberAndMapToResponse(savedCard);
    }

    private PaymentCard mapToEntity(CreateCardRequest request, User user) {
        var card = cardMapper.toEntity(request);
        card.setNumber(cardCryptoService.encrypt(request.number()));
        card.setNumberHash(cardHashService.hash(request.number()));
        card.setUser(user);
        card.setActive(true);

        return card;
    }

    private User fetchUserByIdWithLockOrThrow(Long userId) {
        return userRepository.findByIdWithLock(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id = "+ userId));
    }

    private void validateUserCardLimitOrThrow(Long userId) {
        if (cardRepository.countByUserId(userId) >= cardLimitPerUser) {
            throw new CardLimitExceededException("User cannot have more than " + cardLimitPerUser + " cards.");
        }
    }

    private void validateCardNumberOrThrow(String cardNumber) {
        if (!CardNumberUtils.validate(cardNumber)) {
            throw new InvalidCardNumberException("Card number is invalid");
        }
    }

    private CardResponse decryptCardNumberAndMapToResponse(PaymentCard card) {
        String decryptedNumber = cardCryptoService.decrypt(card.getNumber());
        return cardMapper.toResponse(card, decryptedNumber);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "cards", key = "#id", sync = true)
    public CardResponse getCardById(Long id) {
        var card = fetchCardByIdWithUserOrThrow(id);
        return decryptCardNumberAndMapToResponse(card);
    }

    private PaymentCard fetchCardByIdWithUserOrThrow(Long id) {
        return cardRepository.findByIdWithUser(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CardResponse> getAll(Pageable pageable) {
        return cardRepository.findAllWithUser(pageable)
                .map(this::decryptCardNumberAndMapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "userCards", key = "#userId", sync = true)
    public List<CardResponse> getAllCardsByUserId(Long userId) {
        return cardRepository.findByUserIdWithUser(userId)
                .stream()
                .map(this::decryptCardNumberAndMapToResponse)
                .toList();
    }

    @Override
    @Transactional
    @CachePut(value = "cards", key = "#id")
    @CacheEvict(value = "userCards", key = "#result.userId()")
    public CardResponse updateCard(Long id, UpdateCardRequest request) {
        var card = fetchCardByIdWithUserOrThrow(id);
        cardMapper.updateCardFromDto(request, card);

        if (request.number() != null) {
            validateCardNumberOrThrow(request.number());

            card.setNumber(cardCryptoService.encrypt(request.number()));
            card.setNumberHash(cardHashService.hash(request.number()));
        }

        var savedCard = cardRepository.save(card);
        return decryptCardNumberAndMapToResponse(savedCard);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cards", key = "#id")
    public void deleteCard(Long id) {
        var card = fetchCardByIdOrThrow(id);
        evictUserCardsCache(card.getUser().getId());
        cardRepository.delete(card);
    }

    private PaymentCard fetchCardByIdOrThrow(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new CardNotFoundException("Card not found with id: " + id));
    }

    @Override
    @Transactional
    @CacheEvict(value = "cards", key = "#id")
    public void activateCard(Long id) {
        setActiveStatus(id, true);
    }

    @Override
    @Transactional
    @CacheEvict(value = "cards", key = "#id")
    public void deactivateCard(Long id) {
        setActiveStatus(id, false);
    }

    private void setActiveStatus(Long id, boolean activeStatus) {
        var card = fetchCardByIdOrThrow(id);
        evictUserCardsCache(card.getUser().getId());
        card.setActive(activeStatus);
    }

    private void evictUserCardsCache(Long userId) {
        var cache = cacheManager.getCache("userCards");
        if (cache != null) {
            cache.evict(userId);
        }
    }
}
