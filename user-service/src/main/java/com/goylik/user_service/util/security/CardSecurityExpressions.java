package com.goylik.user_service.util.security;

import com.goylik.user_service.repository.PaymentCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component("cardSecurity")
@RequiredArgsConstructor
public class CardSecurityExpressions {
    private final PaymentCardRepository cardRepository;

    public boolean isCardOwner(Long cardId) {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return cardRepository.findByIdWithUser(cardId)
                .map(card -> card.getUser().getId().equals(currentUserId))
                .orElse(false);
    }
}
