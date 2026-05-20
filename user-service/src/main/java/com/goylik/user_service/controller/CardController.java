package com.goylik.user_service.controller;

import com.goylik.user_service.model.dto.request.CreateCardRequest;
import com.goylik.user_service.model.dto.request.UpdateCardRequest;
import com.goylik.user_service.model.dto.response.CardResponse;
import com.goylik.user_service.service.CardService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@Validated
public class CardController {
    private final CardService cardService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN') or #request.userId == authentication.principal.userId")
    public ResponseEntity<CardResponse> createCard(@Valid @RequestBody CreateCardRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(cardService.createCard(request));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isCardOwner(#id)")
    public ResponseEntity<CardResponse> getCardById(@Positive @PathVariable Long id) {
        return ResponseEntity.ok(cardService.getCardById(id));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<CardResponse>> getAllCards(Pageable pageable) {
        return ResponseEntity.ok(cardService.getAll(pageable));
    }

    @GetMapping("/users/{userId}")
    @PreAuthorize("hasRole('ADMIN') or #userId == authentication.principal.userId")
    public ResponseEntity<List<CardResponse>> getAllCardsByUserId(@Positive @PathVariable Long userId) {
        return ResponseEntity.ok(cardService.getAllCardsByUserId(userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isCardOwner(#id)")
    public ResponseEntity<CardResponse> updateCard(@Positive @PathVariable Long id,
                                   @Valid @RequestBody UpdateCardRequest request) {
        return ResponseEntity.ok(cardService.updateCard(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @cardSecurity.isCardOwner(#id)")
    public ResponseEntity<Void> deleteCard(@Positive @PathVariable Long id) {
        cardService.deleteCard(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> activateCard(@Positive @PathVariable Long id) {
        cardService.activateCard(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deactivateCard(@Positive @PathVariable Long id) {
        cardService.deactivateCard(id);
        return ResponseEntity.noContent().build();

    }
}
