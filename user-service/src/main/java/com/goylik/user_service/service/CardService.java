package com.goylik.user_service.service;

import com.goylik.user_service.model.dto.request.CreateCardRequest;
import com.goylik.user_service.model.dto.request.UpdateCardRequest;
import com.goylik.user_service.model.dto.response.CardResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service for managing payment cards.
 */
public interface CardService {
    /**
     * Creates a new payment card.
     *
     * @param request card creation data
     * @return created card representation
     */
    CardResponse createCard(CreateCardRequest request);

    /**
     * Retrieves a card by its identifier.
     *
     * @param id card identifier
     * @return card representation
     */
    CardResponse getCardById(Long id);

    /**
     * Retrieves a paginated list of all cards.
     *
     * @param pageable pagination parameters
     * @return page of cards
     */
    Page<CardResponse> getAll(Pageable pageable);

    /**
     * Retrieves all cards belonging to a specific user.
     *
     * @param userId user identifier
     * @return list of user's cards
     */
    List<CardResponse> getAllCardsByUserId(Long userId);

    /**
     * Updates an existing card.
     *
     * @param id card identifier
     * @param request card update data
     * @return updated card representation
     */
    CardResponse updateCard(Long id, UpdateCardRequest request);

    /**
     * Deletes an existing card.
     *
     * @param id identifier of card to delete
     */
    void deleteCard(Long id);

    /**
     * Activates a card.
     *
     * @param id card identifier
     */
    void activateCard(Long id);

    /**
     * Deactivates a card.
     *
     * @param id card identifier
     */
    void deactivateCard(Long id);
}