package com.goylik.user_service.controller;

import com.goylik.user_service.client.AuthServiceClient;
import com.goylik.user_service.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
class UserControllerTest extends BaseIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @MockitoBean
    protected AuthServiceClient authServiceClient;

    private static final String INTERNAL_API_KEY = "order-service:key-321321";
    private static final String INTERNAL_API_KEY_HEADER = "X-Internal-Api-Key";


    private static final String BASE_URL = "/api/users";
    private static final String VALID_USER_JSON = """
            {
                "name": "John",
                "surname": "Doe",
                "birthDate": "2000-11-05",
                "email": "john@test.com",
                "password": "password"
            }
            """;

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
    }

    @Test
    void createUser_ShouldReturn201_WhenValidRequest() throws Exception {
        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.surname").value("Doe"))
                .andExpect(jsonPath("$.email").value("john@test.com"))
                .andExpect(jsonPath("$.birthDate").value("2000-11-05"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createUser_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        mockMvc.perform(post(BASE_URL + "/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_USER_JSON));

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.message").value(containsString("already exists")));
    }

    @ParameterizedTest
    @CsvSource({
            "'', Doe, 2000-11-05, john@test.com, password, name is blank",
            "John, Doe, 2000-11-05, not-an-email, password, invalid email format",
            "John, Doe, 3000-01-01, john@test.com, password, future birth date",
            "John, Doe, 2000-11-05, john@test.com, '', password is blank"
    })
    void createUser_ShouldReturn400_ForInvalidInputs(String name, String surname,
                                                     String birthDate, String email,
                                                     String password, String description) throws Exception {
        String invalidJson = """
                {
                    "name": "%s",
                    "surname": "%s",
                    "birthDate": "%s",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(name, surname, birthDate, email, password);

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createAdmin_ShouldReturn403_WhenUserTriesToCreateAdmin() throws Exception {
        mockMvc.perform(withUser(1L, post(BASE_URL + "/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON)))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAdmin_ShouldReturn403_WhenUnauthenticated() throws Exception {
        mockMvc.perform(post(BASE_URL + "/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void createAdmin_ShouldReturn201_WhenAdminCreatesAdmin() throws Exception {
        String adminJson = """
            {
                "name": "Admin",
                "surname": "User",
                "birthDate": "2000-11-05",
                "email": "admin@test.com",
                "password": "password"
            }
            """;

        mockMvc.perform(withAdmin(post(BASE_URL + "/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adminJson)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Admin"))
                .andExpect(jsonPath("$.email").value("admin@test.com"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createAdmin_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        mockMvc.perform(withAdmin(post(BASE_URL + "/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Admin",
                                "surname": "User",
                                "birthDate": "2000-11-05",
                                "email": "admin@test.com",
                                "password": "password"
                            }
                            """)))
                .andExpect(status().isCreated());

        mockMvc.perform(withAdmin(post(BASE_URL + "/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {
                                "name": "Admin2",
                                "surname": "User",
                                "birthDate": "2000-11-05",
                                "email": "admin@test.com",
                                "password": "password"
                            }
                            """)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.message").value(containsString("already exists")));
    }

    @Test
    void createAdmin_ShouldReturn400_WhenInvalidRequest() throws Exception {
        String invalidJson = """
            {
                "name": "",
                "surname": "User",
                "birthDate": "3000-01-01",
                "email": "not-email",
                "password": ""
            }
            """;

        mockMvc.perform(withAdmin(post(BASE_URL + "/admin")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserById_ShouldReturn200_WhenAdminRequests() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("John"))
                .andExpect(jsonPath("$.email").value("john@test.com"));
    }

    @Test
    void getUserById_ShouldReturn200_WhenUserRequestsOwnData() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withUser(userId, get(BASE_URL + "/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId));
    }

    @Test
    void getUserById_ShouldReturn403_WhenUserRequestsOtherUserData() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withUser(999L, get(BASE_URL + "/" + userId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserById_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/99999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("not found")));
    }

    @Test
    void getUserById_ShouldReturn400_WhenIdIsNegative() throws Exception {
        mockMvc.perform(withAdmin(get(BASE_URL + "/-1")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUserByIdInternal_ShouldReturn200_WhenUserExists() throws Exception {
        Long id = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(get(BASE_URL + "/internal/" + id)
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.email").value("john@test.com"))
                .andExpect(jsonPath("$.name").value("John"));
    }

    @Test
    void getUserByIdInternal_ShouldReturn404_WhenUserNotFound() throws Exception {
        mockMvc.perform(get(BASE_URL + "/internal/1111111111")
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message")
                        .value(containsString("not found")));
    }

    @Test
    void getUserByIdInternal_ShouldReturn403_WhenApiKeyIsMissing() throws Exception {
        mockMvc.perform(get(BASE_URL + "/internal/1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserByIdInternal_ShouldReturn403_WhenApiKeyIsWrong() throws Exception {
        mockMvc.perform(get(BASE_URL + "/internal/1")
                        .header("X-Internal-Api-Key", "wrong-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUserByIdInternal_ShouldReturn400_WhenIdIsInvalid() throws Exception {
        mockMvc.perform(get(BASE_URL + "/internal/-999999")
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUsersByIdsInternal_ShouldReturn200_WhenUsersExist() throws Exception {
        Long id1 = createUserAndGetId("John", "Doe", "john@test.com", "password");
        Long id2 = createUserAndGetId("Jane", "Smith", "jane@test.com", "password");

        mockMvc.perform(get(BASE_URL + "/internal")
                        .param("id", String.valueOf(id1))
                        .param("id", String.valueOf(id2))
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[*].id", containsInAnyOrder(id1.intValue(), id2.intValue())))
                .andExpect(jsonPath("$[*].email", containsInAnyOrder("john@test.com", "jane@test.com")));
    }

    @Test
    void getUsersByIdsInternal_ShouldReturn200_WhenSingleId() throws Exception {
        Long id = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(get(BASE_URL + "/internal")
                        .param("id", String.valueOf(id))
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(id));
    }

    @Test
    void getUsersByIdsInternal_ShouldReturn200_WhenSomeIdsDoNotExist() throws Exception {
        Long existingId = createUserAndGetId("John", "Doe", "john@test.com", "password");
        Long nonExistentId = 999999L;

        mockMvc.perform(get(BASE_URL + "/internal")
                        .param("id", String.valueOf(existingId))
                        .param("id", String.valueOf(nonExistentId))
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(existingId));
    }

    @Test
    void getUsersByIdsInternal_ShouldReturn400_WhenIdsEmpty() throws Exception {
        mockMvc.perform(get(BASE_URL + "/internal")
                        .param("id", "")   // пустое значение не может быть преобразовано в Long
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUsersByIdsInternal_ShouldReturn400_WhenNoIdParam() throws Exception {
        mockMvc.perform(get(BASE_URL + "/internal")
                        .header(INTERNAL_API_KEY_HEADER, INTERNAL_API_KEY))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getUsersByIdsInternal_ShouldReturn403_WhenApiKeyMissing() throws Exception {
        mockMvc.perform(get(BASE_URL + "/internal")
                        .param("id", "1"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getUsersByIdsInternal_ShouldReturn403_WhenApiKeyWrong() throws Exception {
        mockMvc.perform(get(BASE_URL + "/internal")
                        .param("id", "1")
                        .header("X-Internal-Api-Key", "wrong-key"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_ShouldReturnPage_WithDefaultPagination() throws Exception {
        createUser("John", "Doe", "john1@test.com", "password");
        createUser("Jane", "Smith", "jane@test.com", "password");
        createUser("Bob", "Johnson", "bob@test.com", "password");

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
    void getAllUsers_ShouldReturn403_WhenUserTriesToGetAll() throws Exception {
        mockMvc.perform(withUser(1L, get(BASE_URL)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllUsers_ShouldReturnFilteredResults_WhenNameProvided() throws Exception {
        createUser("John", "Doe", "john@test.com", "password");
        createUser("Johnny", "Depp", "johnny@test.com", "password");
        createUser("Jane", "Smith", "jane@test.com", "password");

        mockMvc.perform(withAdmin(get(BASE_URL).param("name", "John")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].name", everyItem(containsStringIgnoringCase("John"))));
    }

    @Test
    void getAllUsers_ShouldReturnFilteredResults_WhenSurnameProvided() throws Exception {
        createUser("John", "Doe", "john@test.com", "password");
        createUser("Jane", "Doe", "jane@test.com", "password");
        createUser("Bob", "Smith", "bob@test.com", "password");

        mockMvc.perform(withAdmin(get(BASE_URL).param("surname", "Doe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[*].surname", everyItem(equalTo("Doe"))));
    }

    @Test
    void getAllUsers_ShouldReturnFilteredResults_WhenBothNameAndSurnameProvided() throws Exception {
        createUser("John", "Doe", "john@test.com", "password");
        createUser("John", "Smith", "john.smith@test.com", "password");
        createUser("Jane", "Doe", "jane@test.com", "password");

        mockMvc.perform(withAdmin(get(BASE_URL)
                        .param("name", "John")
                        .param("surname", "Doe")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].name").value("John"))
                .andExpect(jsonPath("$.content[0].surname").value("Doe"));
    }

    @Test
    void getAllUsers_ShouldReturnEmptyPage_WhenNoMatchFound() throws Exception {
        createUser("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withAdmin(get(BASE_URL).param("name", "NonExistent")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0))
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void getAllUsers_ShouldRespectPagination() throws Exception {
        for (int i = 0; i < 15; i++) {
            createUser("User" + i, "Last" + i, "user" + i + "@test.com", "password");
        }

        mockMvc.perform(withAdmin(get(BASE_URL)
                        .param("page", "1")
                        .param("size", "5")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(5))
                .andExpect(jsonPath("$.pageable.pageNumber").value(1))
                .andExpect(jsonPath("$.pageable.pageSize").value(5))
                .andExpect(jsonPath("$.totalElements").value(15))
                .andExpect(jsonPath("$.totalPages").value(3));
    }

    @Test
    void updateUser_ShouldReturn200_WhenAdminUpdates() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        String updateJson = """
                {
                    "name": "Jonathan",
                    "surname": "DoeUpdated",
                    "birthDate": "2000-11-05",
                    "email": "jonathan@test.com"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(userId))
                .andExpect(jsonPath("$.name").value("Jonathan"))
                .andExpect(jsonPath("$.surname").value("DoeUpdated"))
                .andExpect(jsonPath("$.email").value("jonathan@test.com"));
    }

    @Test
    void updateUser_ShouldReturn200_WhenUserUpdatesOwnData() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        String updateJson = """
                {
                    "name": "Jonathan",
                    "surname": "Doe",
                    "birthDate": "2000-11-05",
                    "email": "jonathan@test.com"
                }
                """;

        mockMvc.perform(withUser(userId, put(BASE_URL + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jonathan"));
    }

    @Test
    void updateUser_ShouldReturn403_WhenUserUpdatesOtherUser() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        String updateJson = """
                {
                    "name": "Hacker",
                    "surname": "Doe",
                    "birthDate": "2000-11-05",
                    "email": "hacker@test.com"
                }
                """;

        mockMvc.perform(withUser(999L, put(BASE_URL + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateUser_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        String updateJson = """
                {
                    "name": "Jonathan",
                    "surname": "Doe",
                    "birthDate": "2000-11-05",
                    "email": "jonathan@test.com"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/99999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("not found")));
    }

    @Test
    void updateUser_ShouldReturn409_WhenEmailAlreadyExists() throws Exception {
        createUser("John", "Doe", "john@test.com", "password");
        Long secondUserId = createUserAndGetId("Jane", "Smith", "jane@test.com", "password");

        String updateJson = """
                {
                    "name": "Jane",
                    "surname": "Smith",
                    "birthDate": "2000-11-05",
                    "email": "john@test.com"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/" + secondUserId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.details.message").value(containsString("already exists")));
    }

    @Test
    void updateUser_ShouldAllowUpdatingWithSameEmail() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        String updateJson = """
                {
                    "name": "Jonathan",
                    "surname": "Doe",
                    "birthDate": "2000-11-05",
                    "email": "john@test.com"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jonathan"))
                .andExpect(jsonPath("$.email").value("john@test.com"));
    }

    @Test
    void updateUser_ShouldReturn400_WhenInvalidData() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        String invalidJson = """
                {
                    "name": "",
                    "surname": "Doe",
                    "birthDate": "3000-01-01",
                    "email": "not-email"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteUser_ShouldReturn204_WhenAdminDeletes() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + userId)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    void deleteUser_ShouldReturn204_WhenUserDeletesOwnAccount() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withUser(userId, delete(BASE_URL + "/" + userId)))
                .andExpect(status().isNoContent());

        assertThat(userRepository.findById(userId)).isEmpty();
    }

    @Test
    void deleteUser_ShouldReturn403_WhenUserDeletesOtherUser() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withUser(999L, delete(BASE_URL + "/" + userId)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deleteUser_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(delete(BASE_URL + "/99999")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("not found")));
    }

    @Test
    void deleteUser_ShouldReturn404_WhenUserAlreadyDeleted() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + userId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + userId)))
                .andExpect(status().isNotFound());
    }

    @Test
    void activateUser_ShouldReturn204_WhenUserExists() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + userId + "/deactivate")))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + userId + "/activate")))
                .andExpect(status().isNoContent());

        var user = userRepository.findById(userId);
        assertThat(user).isPresent();
        assertThat(user.get().getActive()).isTrue();
    }

    @Test
    void deactivateUser_ShouldReturn204_WhenUserExists() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + userId + "/deactivate")))
                .andExpect(status().isNoContent());

        var user = userRepository.findById(userId);
        assertThat(user).isPresent();
        assertThat(user.get().getActive()).isFalse();
    }

    @Test
    void activateUser_ShouldReturn403_WhenUserTriesToActivate() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withUser(userId, patch(BASE_URL + "/" + userId + "/activate")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deactivateUser_ShouldReturn403_WhenUserTriesToDeactivate() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withUser(userId, patch(BASE_URL + "/" + userId + "/deactivate")))
                .andExpect(status().isForbidden());
    }

    @Test
    void activateUser_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(patch(BASE_URL + "/99999/activate")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("not found")));
    }

    @Test
    void deactivateUser_ShouldReturn404_WhenUserDoesNotExist() throws Exception {
        mockMvc.perform(withAdmin(patch(BASE_URL + "/99999/deactivate")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.details.message").value(containsString("not found")));
    }

    @Test
    void activateUser_ShouldWork_WhenUserAlreadyActive() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + userId + "/activate")))
                .andExpect(status().isNoContent());

        var user = userRepository.findById(userId);
        assertThat(user).isPresent();
        assertThat(user.get().getActive()).isTrue();
    }

    @Test
    void deactivateUser_ShouldWork_WhenUserAlreadyDeactivated() throws Exception {
        Long userId = createUserAndGetId("John", "Doe", "john@test.com", "password");

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + userId + "/deactivate")))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + userId + "/deactivate")))
                .andExpect(status().isNoContent());

        var user = userRepository.findById(userId);
        assertThat(user).isPresent();
        assertThat(user.get().getActive()).isFalse();
    }

    @Test
    void completeUserLifecycle_ShouldWork() throws Exception {
        MvcResult createResult = mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_USER_JSON))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdUser = objectMapper.readTree(createResult.getResponse().getContentAsString());
        Long userId = createdUser.get("id").asLong();

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("John"));

        String updateJson = """
                {
                    "name": "Jonathan",
                    "surname": "Doe",
                    "birthDate": "2000-11-05",
                    "email": "john.updated@test.com"
                }
                """;

        mockMvc.perform(withAdmin(put(BASE_URL + "/" + userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateJson)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Jonathan"))
                .andExpect(jsonPath("$.email").value("john.updated@test.com"));

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + userId + "/deactivate")))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(withAdmin(patch(BASE_URL + "/" + userId + "/activate")))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + userId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(withAdmin(delete(BASE_URL + "/" + userId)))
                .andExpect(status().isNoContent());

        mockMvc.perform(withAdmin(get(BASE_URL + "/" + userId)))
                .andExpect(status().isNotFound());
    }

    private void createUser(String name, String surname, String email, String password) throws Exception {
        String userJson = """
                {
                    "name": "%s",
                    "surname": "%s",
                    "birthDate": "2000-11-05",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(name, surname, email, password);

        mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated());
    }

    private Long createUserAndGetId(String name, String surname, String email, String password) throws Exception {
        String userJson = """
                {
                    "name": "%s",
                    "surname": "%s",
                    "birthDate": "2000-11-05",
                    "email": "%s",
                    "password": "%s"
                }
                """.formatted(name, surname, email, password);

        MvcResult result = mockMvc.perform(post(BASE_URL + "/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(userJson))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdUser = objectMapper.readTree(result.getResponse().getContentAsString());
        return createdUser.get("id").asLong();
    }
}