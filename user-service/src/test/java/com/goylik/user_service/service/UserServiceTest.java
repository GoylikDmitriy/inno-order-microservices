package com.goylik.user_service.service;

import com.goylik.user_service.client.AuthServiceClient;
import com.goylik.user_service.exception.client.AuthServiceUnavailableException;
import com.goylik.user_service.exception.user.UserAlreadyExistsException;
import com.goylik.user_service.exception.user.UserNotFoundException;
import com.goylik.user_service.mapper.UserMapper;
import com.goylik.user_service.model.dto.request.CreateUserRequest;
import com.goylik.user_service.model.dto.request.UpdateUserRequest;
import com.goylik.user_service.model.dto.response.UserResponse;
import com.goylik.user_service.model.entity.User;
import com.goylik.user_service.model.enums.Role;
import com.goylik.user_service.repository.UserRepository;
import com.goylik.user_service.service.impl.UserServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private UserMapper userMapper;
    @Mock private AuthServiceClient authServiceClient;

    @InjectMocks private UserServiceImpl userService;

    private User user;
    private UserResponse response;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1L);
        user.setName("John");
        user.setSurname("Doe");
        user.setEmail("john@mail.com");
        user.setBirthDate(LocalDate.of(2000, 11, 5));
        user.setActive(true);

        response = new UserResponse(
                1L,
                "John",
                "Doe",
                LocalDate.of(2000, 11, 5),
                "john@mail.com",
                true
        );
    }

    @Test
    void createUser_shouldCreateUserSuccessfully() {
        CreateUserRequest request = new CreateUserRequest(
                "John",
                "Doe",
                LocalDate.of(2000, 11, 5),
                "john@mail.com",
                "password"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.createUser(request, Role.ROLE_USER);

        assertNotNull(result);
        assertEquals(response.id(), result.id());

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void createUser_shouldThrowExceptionWhenEmailExists() {
        CreateUserRequest request = new CreateUserRequest(
                "John",
                "Doe",
                LocalDate.of(2000, 11, 5),
                "john@mail.com",
                "password"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(true);

        assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.createUser(request, Role.ROLE_USER)
        );

        verify(userRepository).existsByEmail(request.email());
        verify(userRepository, never()).save(any());
    }

    @Test
    void getUserById_shouldReturnUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.getUserById(1L);

        assertNotNull(result);
        assertEquals(1L, result.id());

        verify(userRepository).findById(1L);
        verify(userMapper).toResponse(user);
    }

    @Test
    void getUserById_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.getUserById(1L)
        );

        verify(userRepository).findById(1L);
    }

    @Test
    void getUsersByIds_shouldReturnListOfUsers_whenIdsExist() {
        List<Long> ids = List.of(1L, 2L);
        User user2 = new User();
        user2.setId(2L);
        user2.setName("Jane");
        user2.setSurname("Smith");
        user2.setEmail("jane@mail.com");
        user2.setBirthDate(LocalDate.of(1995, 5, 15));
        user2.setActive(true);

        UserResponse response2 = new UserResponse(
                2L, "Jane", "Smith", LocalDate.of(1995, 5, 15), "jane@mail.com", true
        );

        when(userRepository.findAllById(ids)).thenReturn(List.of(user, user2));
        when(userMapper.toResponse(user)).thenReturn(response);
        when(userMapper.toResponse(user2)).thenReturn(response2);

        List<UserResponse> result = userService.getUsersByIds(ids);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).id());
        assertEquals(2L, result.get(1).id());

        verify(userRepository).findAllById(ids);
        verify(userMapper, times(2)).toResponse(any(User.class));
    }

    @Test
    void getUsersByIds_shouldReturnOnlyExistingUsers_whenSomeIdsDoNotExist() {
        List<Long> ids = List.of(1L, 999L);
        when(userRepository.findAllById(ids)).thenReturn(List.of(user));
        when(userMapper.toResponse(user)).thenReturn(response);

        List<UserResponse> result = userService.getUsersByIds(ids);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).id());

        verify(userRepository).findAllById(ids);
        verify(userMapper, times(1)).toResponse(user);
    }

    @Test
    void getUsersByIds_shouldReturnEmptyList_whenNoUsersFound() {
        List<Long> ids = List.of(999L, 1000L);
        when(userRepository.findAllById(ids)).thenReturn(List.of());

        List<UserResponse> result = userService.getUsersByIds(ids);

        assertNotNull(result);
        assertTrue(result.isEmpty());

        verify(userRepository).findAllById(ids);
        verify(userMapper, never()).toResponse(any());
    }

    @Test
    void getAll_shouldReturnPageOfUsers() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<User> userPage = new PageImpl<>(List.of(user));

        when(userRepository.findAll(any(Specification.class), eq(pageable))).thenReturn(userPage);
        when(userMapper.toResponse(user)).thenReturn(response);

        Page<UserResponse> result = userService.getAll(pageable, "John", "Doe");

        assertEquals(1, result.getTotalElements());

        verify(userRepository).findAll(any(Specification.class), eq(pageable));
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUser_shouldUpdateUserSuccessfully() {
        UpdateUserRequest request = new UpdateUserRequest("Jane", "Doe", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        UserResponse result = userService.updateUser(1L, request);

        assertNotNull(result);

        verify(userMapper).updateUserFromDto(request, user);
        verify(userRepository).save(user);
        verify(userMapper).toResponse(user);
    }

    @Test
    void updateUser_shouldThrowExceptionWhenUserNotFound() {
        UpdateUserRequest request = new UpdateUserRequest("Jane", "Doe", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.updateUser(1L, request)
        );
    }

    @Test
    void deleteUser_shouldDeleteUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deleteUser(1L);

        verify(userRepository).delete(user);
    }

    @Test
    void deleteUser_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.deleteUser(1L)
        );
    }

    @Test
    void activateUser_shouldActivateUser() {
        user.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.activateUser(1L);

        assertTrue(user.getActive());
    }

    @Test
    void activateUser_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.activateUser(1L)
        );
    }

    @Test
    void deactivateUser_shouldDeactivateUser() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deactivateUser(1L);

        assertFalse(user.getActive());
    }

    @Test
    void deactivateUser_shouldThrowExceptionWhenUserNotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> userService.deactivateUser(1L)
        );
    }

    @Test
    void createUser_shouldCallAuthServiceClient() {
        CreateUserRequest request = new CreateUserRequest(
                "John", "Doe", LocalDate.of(2000, 11, 5),
                "john@mail.com", "password"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        userService.createUser(request, Role.ROLE_USER);

        verify(authServiceClient).saveCredentials(argThat(saved ->
                saved.userId().equals(1L) &&
                        saved.email().equals("john@mail.com") &&
                        saved.password().equals("password") &&
                        saved.role().equals(Role.ROLE_USER)
        ));
    }

    @Test
    void createUser_shouldPropagateException_WhenAuthServiceFails() {
        CreateUserRequest request = new CreateUserRequest(
                "John", "Doe", LocalDate.of(2000, 11, 5),
                "john@mail.com", "password"
        );

        when(userRepository.existsByEmail(request.email())).thenReturn(false);
        when(userMapper.toEntity(request)).thenReturn(user);
        when(userRepository.save(user)).thenReturn(user);
        doThrow(new AuthServiceUnavailableException("Auth service is unavailable"))
                .when(authServiceClient).saveCredentials(any());

        assertThrows(
                AuthServiceUnavailableException.class,
                () -> userService.createUser(request, Role.ROLE_USER)
        );
    }

    @Test
    void updateUser_shouldThrowException_WhenNewEmailAlreadyExists() {
        UpdateUserRequest request = new UpdateUserRequest(
                "Jane", "Doe", null, "existing@mail.com"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.existsByEmail("existing@mail.com")).thenReturn(true);

        assertThrows(
                UserAlreadyExistsException.class,
                () -> userService.updateUser(1L, request)
        );

        verify(userRepository, never()).save(any());
    }

    @Test
    void updateUser_shouldNotValidateEmail_WhenEmailIsUnchanged() {
        UpdateUserRequest request = new UpdateUserRequest(
                "Jane", "Doe", null, "john@mail.com"
        );

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        userService.updateUser(1L, request);

        verify(userRepository, never()).existsByEmail(any());
        verify(userRepository).save(user);
    }

    @Test
    void updateUser_shouldNotValidateEmail_WhenEmailIsNull() {
        UpdateUserRequest request = new UpdateUserRequest("Jane", "Doe", null, null);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(userMapper.toResponse(user)).thenReturn(response);

        userService.updateUser(1L, request);

        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void deactivateUser_shouldWork_WhenUserAlreadyDeactivated() {
        user.setActive(false);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.deactivateUser(1L);

        assertFalse(user.getActive());
    }

    @Test
    void activateUser_shouldWork_WhenUserAlreadyActive() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        userService.activateUser(1L);

        assertTrue(user.getActive());
    }
}