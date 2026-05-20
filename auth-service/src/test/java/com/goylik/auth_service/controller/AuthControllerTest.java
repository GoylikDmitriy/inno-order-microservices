package com.goylik.auth_service.controller;

import com.goylik.auth_service.model.entity.UserCredentials;
import com.goylik.auth_service.model.enums.Role;
import com.goylik.auth_service.repository.UserCredentialsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class AuthControllerTest extends BaseIntegrationTest {
    @Autowired private UserCredentialsRepository userCredentialsRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private static final String INTERNAL_API_KEY = "gateway:key-312312";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";

    private static final String BASE_URL = "/api/auth";

    private static final String VALID_CREDENTIALS_JSON = """
            {
                "userId": 1,
                "email": "john@test.com",
                "password": "password",
                "role": "ROLE_USER"
            }
            """;

    @BeforeEach
    void setUp() {
        userCredentialsRepository.deleteAll();
    }

    @Test
    void saveCredentials_ShouldReturn201_WhenValidRequest() throws Exception {
        mockMvc.perform(post(BASE_URL + "/save-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY)
                        .content(VALID_CREDENTIALS_JSON))
                .andExpect(status().isCreated());

        assertThat(userCredentialsRepository.existsByEmail("john@test.com")).isTrue();
    }

    @Test
    void saveCredentials_ShouldHashPassword_BeforeSaving() throws Exception {
        mockMvc.perform(post(BASE_URL + "/save-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY)
                        .content(VALID_CREDENTIALS_JSON))
                .andExpect(status().isCreated());

        var saved = userCredentialsRepository.findByEmail("john@test.com");
        assertThat(saved).isPresent();
        assertThat(saved.get().getPassword()).isNotEqualTo("password");
        assertThat(passwordEncoder.matches("password", saved.get().getPassword())).isTrue();
    }

    @Test
    void saveCredentials_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        mockMvc.perform(post(BASE_URL + "/save-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY)
                        .content(VALID_CREDENTIALS_JSON))
                .andExpect(status().isCreated());

        mockMvc.perform(post(BASE_URL + "/save-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY)
                        .content(VALID_CREDENTIALS_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void saveCredentials_ShouldReturn409_WhenUserIdAlreadyExists() throws Exception {
        mockMvc.perform(post(BASE_URL + "/save-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY)
                        .content(VALID_CREDENTIALS_JSON))
                .andExpect(status().isCreated());

        String differentEmailSameUserId = """
                {
                    "userId": 1,
                    "email": "other@test.com",
                    "password": "password",
                    "role": "ROLE_USER"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/save-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY)
                        .content(differentEmailSameUserId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void saveCredentials_ShouldReturn400_WhenInvalidRequest() throws Exception {
        String invalidJson = """
                {
                    "userId": null,
                    "email": "not-an-email",
                    "password": "",
                    "role": null
                }
                """;

        mockMvc.perform(post(BASE_URL + "/save-credentials")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void login_ShouldReturnTokens_WhenValidCredentials() throws Exception {
        saveCredentials("john@test.com", "password", Role.ROLE_USER);

        String loginJson = """
                {
                    "email": "john@test.com",
                    "password": "password"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void login_ShouldReturn401_WhenWrongPassword() throws Exception {
        saveCredentials("john@test.com", "password", Role.ROLE_USER);

        String loginJson = """
                {
                    "email": "john@test.com",
                    "password": "wrong_password"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_ShouldReturn401_WhenEmailNotFound() throws Exception {
        String loginJson = """
                {
                    "email": "unknown@test.com",
                    "password": "password"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_ShouldReturn401_WhenAccountIsDisabled() throws Exception {
        saveCredentials("john@test.com", "password", Role.ROLE_USER, false);

        String loginJson = """
                {
                    "email": "john@test.com",
                    "password": "password"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void login_ShouldReturn400_WhenInvalidRequest() throws Exception {
        String invalidJson = """
                {
                    "email": "",
                    "password": ""
                }
                """;

        mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void validateToken_ShouldReturnValid_WhenTokenIsValid() throws Exception {
        String accessToken = loginAndGetAccessToken("john@test.com", "password");

        String validateJson = """
                {
                    "token": "%s"
                }
                """.formatted(accessToken);

        mockMvc.perform(post(BASE_URL + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void validateToken_ShouldReturnInvalid_WhenTokenIsExpired() throws Exception {
        String validateJson = """
                {
                    "token": "invalid.token.here"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.userId").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    void validateToken_ShouldReturn400_WhenTokenIsBlank() throws Exception {
        String validateJson = """
                {
                    "token": ""
                }
                """;

        mockMvc.perform(post(BASE_URL + "/validate")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validateJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void refreshToken_ShouldReturnNewTokens_WhenRefreshTokenIsValid() throws Exception {
        String refreshToken = loginAndGetRefreshToken("john@test.com", "password");

        String refreshJson = """
                {
                    "refreshToken": "%s"
                }
                """.formatted(refreshToken);

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty());
    }

    @Test
    void refreshToken_ShouldReturn401_WhenRefreshTokenIsInvalid() throws Exception {
        String refreshJson = """
                {
                    "refreshToken": "invalid.token.here"
                }
                """;

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void refreshToken_ShouldReturn400_WhenRefreshTokenIsBlank() throws Exception {
        String refreshJson = """
                {
                    "refreshToken": ""
                }
                """;

        mockMvc.perform(post(BASE_URL + "/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isBadRequest());
    }

    private void saveCredentials(String email, String password, Role role) {
        saveCredentials(email, password, role, true);
    }

    private void saveCredentials(String email, String password, Role role, boolean active) {
        var credentials = new UserCredentials();
        credentials.setUserId(1L);
        credentials.setEmail(email);
        credentials.setPassword(passwordEncoder.encode(password));
        credentials.setRole(role);
        credentials.setActive(active);
        userCredentialsRepository.save(credentials);
    }

    private String loginAndGetAccessToken(String email, String password) throws Exception {
        saveCredentials(email, password, Role.ROLE_USER);

        String loginJson = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

    private String loginAndGetRefreshToken(String email, String password) throws Exception {
        saveCredentials(email, password, Role.ROLE_USER);

        String loginJson = """
                {
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(email, password);

        MvcResult result = mockMvc.perform(post(BASE_URL + "/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("refreshToken").asText();
    }
}