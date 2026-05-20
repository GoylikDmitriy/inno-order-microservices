package com.goylik.user_service.service;

import com.goylik.user_service.model.dto.request.CreateUserRequest;
import com.goylik.user_service.model.dto.request.UpdateUserRequest;
import com.goylik.user_service.model.dto.response.UserResponse;
import com.goylik.user_service.model.enums.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Service interface for managing users.
 * Provides operations for creating, retrieving, updating
 * and managing the active status of users.
 */
public interface UserService {
    /**
     * Creates a new user with determined {@link Role}.
     *
     * @param request DTO containing user creation data
     * @return created user representation
     */
    UserResponse createUser(CreateUserRequest request, Role role);

    /**
     * Retrieves a user by its identifier.
     *
     * @param id user identifier
     * @return user representation
     */
    UserResponse getUserById(Long id);

    /**
     * Retrieves a paginated list of users with optional filtering
     * by name and surname.
     *
     * @param pageable pagination parameters
     * @param name optional user name filter
     * @param surname optional user surname filter
     * @return page of users
     */
    Page<UserResponse> getAll(Pageable pageable, String name, String surname);

    /**
     * Updates an existing user.
     *
     * @param id identifier of user to update
     * @param request DTO containing updated data
     * @return updated user representation
     */
    UserResponse updateUser(Long id, UpdateUserRequest request);

    /**
     * Deletes an existing user.
     *
     * @param id identifier of user to delete
     */
    void deleteUser(Long id);

    /**
     * Activates user account.
     *
     * @param id user identifier
     */
    void activateUser(Long id);

    /**
     * Deactivates user account.
     *
     * @param id user identifier
     */
    void deactivateUser(Long id);

    /**
     * Retrieves all users by identifiers.
     *
     * @param ids user identifiers
     * @return list of user representations
     */
    List<UserResponse> getUsersByIds(List<Long> ids);
}