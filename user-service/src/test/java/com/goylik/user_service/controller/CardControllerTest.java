package com.goylik.user_service.controller;

import com.goylik.user_service.model.dto.response.CardResponse;
import com.goylik.user_service.model.entity.User;
import com.goylik.user_service.repository.PaymentCardRepository;
import com.goylik.user_service.repository.UserRepository;
import com.goylik.user_service.service.CardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class CardControllerTest extends BaseIntegrationTest {

    @Autowired
    private CardService cardService;

    @Autowired
    private PaymentCardRepository cardRepository;

    @Autowired
    private UserRepository userRepository;

    private Long userId;

    private static final String BASE_URL = "/api/cards";
    private static final String VALID_CARD_JSON = """
            {
                "userId": %d,
                "number": "4532015112830366",
                "holder": "John Doe",
                "expirationDate": "2026-12"
            }
            """;

    private static final List<String> VALID_NUMBERS = List.of(
            "4532015112830366",
            "5555555555554444",
            "4111111111111111",
            "4012888888881881",
            "4222222222222220",
            "4444333322221111",
            "4716100000000008",
            "4484070000000000",
            "4917610000000000",
            "4929430000000006",
            "4485740000000000",
            "4532610000000006",
            "4911830000000000",
            "4921810000000006",
            "4539970000000000"
    );

    @BeforeEach
    void setUp() {
        cardRepository.deleteAll();
        userRepository.deleteAll();

        User user = new User();
        user.setName("John");
        user.setSurname("Doe");
        user.setEmail("john@test.com");
        user.setBirthDate(LocalDate.of(2001, 11, 5));
        user.setActive(true);
        userId = userRepository.save(user).getId();
    }

    @Test
    void createCard_ShouldReturn201_WhenAdminCreatesCard() throws Exception {
        String request = String.format(VALID_CARD_JSON, userId);

        mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.holder").value("John Doe"))
                .andExpect(jsonPath("$.expirationDate").value("2026-12"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createCard_ShouldReturn201_WhenUserCreatesOwnCard() throws Exception {
        String request = String.format(VALID_CARD_JSON, userId);

        mockMvc.perform(withUser(userId, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(userId));
    }

    @Test
    void createCard_ShouldReturn403_WhenUserCreatesCardForOtherUser() throws Exception {
        String request = String.format(VALID_CARD_JSON, userId);

        mockMvc.perform(withUser(999L, post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createCard_ShouldReturn400_WhenUserHasMaxCards() throws Exception {
        for (int i = 0; i < 5; i++) {
            createCard(userId, VALID_NUMBERS.get(i));
        }

        String request = String.format(VALID_CARD_JSON, userId);

        mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.message").value(containsString("cannot have more than 5 cards")));
    }

    @Test
    void createCard_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        String request = String.format(VALID_CARD_JSON, 99999L);

        mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("User not found")));
    }

    @Test
    void createCard_ShouldReturn400_WhenCardNumberIsInvalid() throws Exception {
        String invalidRequest = """
                {
                    "userId": %d,
                    "number": "1234",
                    "holder": "John Doe",
                    "expirationDate": "2026-12"
                }
                """.formatted(userId);

        mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.message").value(containsString("Card number is invalid")));
    }

    @Test
    void createCard_ShouldReturn400_WhenHolderIsBlank() throws Exception {
        String invalidRequest = """
                {
                    "userId": %d,
                    "number": "4532015112830366",
                    "holder": "",
                    "expirationDate": "2026-12"
                }
                """.formatted(userId);

        mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getCardById_ShouldReturn200_WhenAdminRequests() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + cardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.holder").value("John Doe"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void getCardById_ShouldReturn200_WhenOwnerRequests() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withUser(userId, get(BASE_URL + "/" + cardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId));
    }

    @Test
    void getCardById_ShouldReturn403_WhenUserRequestsOtherUsersCard() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withUser(999L, get(BASE_URL + "/" + cardId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getCardById_ShouldReturn404_WhenCardDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/99999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Card not found")));
    }

    @Test
    void getCardById_ShouldReturn400_WhenIdIsNegative() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAllCards_ShouldReturnPage_WithDefaultPagination() throws Exception {
        createCard(userId, "4532015112830366");
        createCard(userId, "5555555555554444");
        createCard(userId, "4111111111111111");

        mockMvc.perform(withAdmin(get(BASE_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(3))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageable.pageNumber").value(0))
                .andExpect(jsonPath("$.pageable.pageSize").value(20));
    }

    @Test
    void getAllCards_ShouldReturn403_WhenUserTriesToGetAll() throws Exception {
        mockMvc.perform(withUser(userId, get(BASE_URL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCards_ShouldReturnEmptyPage_WhenNoCards() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllCardsByUserId_ShouldReturnList_WhenAdminRequests() throws Exception {
        createCard(userId, "4532015112830366");
        createCard(userId, "5555555555554444");

        mockMvc.perform(withAdmin(get(BASE_URL + "/users/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[1].userId").value(userId));
    }

    @Test
    void getAllCardsByUserId_ShouldReturnList_WhenOwnerRequests() throws Exception {
        createCard(userId, "4532015112830366");
        createCard(userId, "5555555555554444");

        mockMvc.perform(withUser(userId, get(BASE_URL + "/users/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getAllCardsByUserId_ShouldReturn403_WhenUserRequestsOtherUsersCards() throws Exception {
        mockMvc.perform(withUser(999L, get(BASE_URL + "/users/" + userId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllCardsByUserId_ShouldReturnEmptyList_WhenUserHasNoCards() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/users/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllCardsByUserId_ShouldReturn200_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/users/99999")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getAllCardsByUserId_ShouldReturn400_WhenUserIdIsNegative() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/users/-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void updateCard_ShouldReturn200_WhenAdminUpdates() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        String updateRequest = """
                {
                    "number": "5555555555554444",
                    "holder": "Jane Doe",
                    "expirationDate": "2026-12"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(cardId))
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.holder").value("Jane Doe"))
                .andExpect(jsonPath("$.expirationDate").value("2026-12"));
    }

    @Test
    void updateCard_ShouldReturn200_WhenOwnerUpdates() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        String updateRequest = """
                {
                    "holder": "Jonathan Doe",
                    "expirationDate": "2026-12"
                }
                """;

        mockMvc.perform(withUser(userId, put(BASE_URL + "/" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("Jonathan Doe"));
    }

    @Test
    void updateCard_ShouldReturn403_WhenUserUpdatesOtherUsersCard() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        String updateRequest = """
                {
                    "holder": "Hacker",
                    "expirationDate": "2026-12"
                }
                """;

        mockMvc.perform(withUser(999L, put(BASE_URL + "/" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateCard_ShouldReturn404_WhenCardDoesNotExist() throws Exception {
        String updateRequest = """
                {
                    "number": "5555555555554444",
                    "holder": "Jane Doe",
                    "expirationDate": "2026-12"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Card not found")));
    }

    @Test
    void updateCard_ShouldReturn400_WhenInvalidData() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        String invalidRequest = """
                {
                    "number": "1234",
                    "holder": "",
                    "expirationDate": "2020-01"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCard_ShouldReturn204_WhenAdminDeletes() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + cardId)))
                .andExpect(status().isNoContent());

        assertThat(cardRepository.findById(cardId)).isEmpty();
    }

    @Test
    void deleteCard_ShouldReturn204_WhenOwnerDeletes() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withUser(userId, delete(BASE_URL + "/" + cardId)))
                .andExpect(status().isNoContent());

        assertThat(cardRepository.findById(cardId)).isEmpty();
    }

    @Test
    void deleteCard_ShouldReturn403_WhenUserDeletesOtherUsersCard() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withUser(999L, delete(BASE_URL + "/" + cardId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteCard_ShouldReturn404_WhenCardDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(delete(BASE_URL + "/99999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Card not found")));
    }

    @Test
    void deleteCard_ShouldReturn404_WhenCardAlreadyDeleted() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + cardId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + cardId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void activateCard_ShouldReturn204_WhenAdminActivates() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + cardId + "/deactivate")))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + cardId + "/activate")))
                .andExpect(status().isNoContent());

        CardResponse card = cardService.getCardById(cardId);
        assertThat(card.active()).isTrue();
    }

    @Test
    void deactivateCard_ShouldReturn204_WhenAdminDeactivates() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + cardId + "/deactivate")))
                .andExpect(status().isNoContent());

        CardResponse card = cardService.getCardById(cardId);
        assertThat(card.active()).isFalse();
    }

    @Test
    void activateCard_ShouldReturn403_WhenUserTriesToActivate() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withUser(userId, patch(BASE_URL + "/" + cardId + "/activate")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateCard_ShouldReturn403_WhenUserTriesToDeactivate() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withUser(userId, patch(BASE_URL + "/" + cardId + "/deactivate")))
                .andExpect(status().isForbidden());
    }

    @Test
    void activateCard_ShouldReturn404_WhenCardDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(patch(BASE_URL + "/99999/activate")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Card not found")));
    }

    @Test
    void deactivateCard_ShouldReturn404_WhenCardDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(patch(BASE_URL + "/99999/deactivate")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("Card not found")));
    }

    @Test
    void activateCard_ShouldWork_WhenCardAlreadyActive() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + cardId + "/activate")))
                .andExpect(status().isNoContent());

        CardResponse card = cardService.getCardById(cardId);
        assertThat(card.active()).isTrue();
    }

    @Test
    void deactivateCard_ShouldWork_WhenCardAlreadyDeactivated() throws Exception {
        Long cardId = createCardAndGetId(userId, "4532015112830366");

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + cardId + "/deactivate")))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + cardId + "/deactivate")))
                .andExpect(status().isNoContent());

        CardResponse card = cardService.getCardById(cardId);
        assertThat(card.active()).isFalse();
    }

    @Test
    void completeCardLifecycle_ShouldWork() throws Exception {
        String request = String.format(VALID_CARD_JSON, userId);

        MvcResult createResult = mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdCard = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long cardId = createdCard.get("id").asLong();

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + cardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("John Doe"));

        String updateRequest = """
                {
                    "holder": "Jonathan Doe",
                    "expirationDate": "2026-12"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/" + cardId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.holder").value("Jonathan Doe"))
                .andExpect(jsonPath("$.expirationDate").value("2026-12"));

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + cardId + "/deactivate")))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + cardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + cardId + "/activate")))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + cardId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(withAdmin(get(BASE_URL + "/users/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].id").value(cardId));

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + cardId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + cardId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void userWithCards_ShouldSeeOnlyOwnCards() throws Exception {
        User user2 = new User();
        user2.setName("Jane");
        user2.setSurname("Smith");
        user2.setEmail("jane@test.com");
        user2.setBirthDate(LocalDate.of(2000, 11, 5));
        user2.setActive(true);
        Long userId2 = userRepository.save(user2).getId();

        createCard(userId, "4532015112830366");
        createCard(userId, "5555555555554444");
        createCard(userId2, "4111111111111111");

        mockMvc.perform(withUser(userId, get(BASE_URL + "/users/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].userId").value(userId))
                .andExpect(jsonPath("$[1].userId").value(userId));

        mockMvc.perform(withUser(userId2, get(BASE_URL + "/users/" + userId2)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].userId").value(userId2));

        mockMvc.perform(withUser(userId, get(BASE_URL + "/users/" + userId2)))
                .andExpect(status().isForbidden());
    }

    private void createCard(Long userId, String cardNumber) throws Exception {
        String request = """
                {
                    "userId": %d,
                    "number": "%s",
                    "holder": "John Doe",
                    "expirationDate": "2026-12"
                }
                """.formatted(userId, cardNumber);

        mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated());
    }

    private Long createCardAndGetId(Long userId, String cardNumber) throws Exception {
        String request = """
                {
                    "userId": %d,
                    "number": "%s",
                    "holder": "John Doe",
                    "expirationDate": "2026-12"
                }
                """.formatted(userId, cardNumber);

        MvcResult result = mockMvc.perform(withAdmin(post(BASE_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdCard = objectMapper.readTree(result.getResponse().getContentAsString());
        return createdCard.get("id").asLong();
    }
}