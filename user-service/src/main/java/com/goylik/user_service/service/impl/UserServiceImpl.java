package com.goylik.user_service.service.impl;

import com.goylik.user_service.client.AuthServiceClient;
import com.goylik.user_service.exception.user.UserAlreadyExistsException;
import com.goylik.user_service.exception.user.UserNotFoundException;
import com.goylik.user_service.mapper.UserMapper;
import com.goylik.user_service.model.dto.client.SaveCredentialsRequest;
import com.goylik.user_service.model.dto.request.CreateUserRequest;
import com.goylik.user_service.model.dto.request.UpdateUserRequest;
import com.goylik.user_service.model.dto.response.UserResponse;
import com.goylik.user_service.model.entity.User;
import com.goylik.user_service.model.enums.Role;
import com.goylik.user_service.repository.UserRepository;
import com.goylik.user_service.service.UserService;
import com.goylik.user_service.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
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
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final UserMapper userMapper;

    private final AuthServiceClient authServiceClient;

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserResponse createUser(CreateUserRequest request, Role role) {
        validateEmailNotExistsOrThrow(request.email());

        User user = userMapper.toEntity(request);
        user.setActive(true);

        User savedUser = userRepository.save(user);

        saveCredentials(savedUser, request.password(), role);

        return userMapper.toResponse(savedUser);
    }

    private void validateEmailNotExistsOrThrow(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new UserAlreadyExistsException("User with this email already exists");
        }
    }

    private void saveCredentials(User user, String password, Role role) {
        authServiceClient.saveCredentials(new SaveCredentialsRequest(
                user.getId(),
                user.getEmail(),
                password,
                role));
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "users", key = "#id", sync = true)
    public UserResponse getUserById(Long id) {
        User user = fetchUserByIdOrThrow(id);
        return userMapper.toResponse(user);
    }

    private User fetchUserByIdOrThrow(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> getAll(Pageable pageable, String name, String surname) {
        return userRepository
                .findAll(UserSpecification.hasFirstNameLikeAndHasSurnameLike(name, surname), pageable)
                .map(userMapper::toResponse);
    }

    @Override
    @Transactional
    @CachePut(value = "users", key = "#result.id")
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = fetchUserByIdOrThrow(id);
        if (request.email() != null && !request.email().equals(user.getEmail())) {
            validateEmailNotExistsOrThrow(request.email());
        }

        userMapper.updateUserFromDto(request, user);

        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deleteUser(Long id) {
        var user = fetchUserByIdOrThrow(id);
        userRepository.delete(user);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void activateUser(Long id) {
        setActiveStatus(id, true);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#id")
    public void deactivateUser(Long id) {
        setActiveStatus(id, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByIds(List<Long> ids) {
        List<User> users = userRepository.findAllById(ids);
        return users.stream()
                .map(userMapper::toResponse)
                .toList();
    }

    private void setActiveStatus(Long id, boolean activeStatus) {
        User user = fetchUserByIdOrThrow(id);
        user.setActive(activeStatus);
    }
}