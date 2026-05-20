package com.goylik.auth_service.repository;

import com.goylik.auth_service.model.entity.UserCredentials;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserCredentialsRepository extends JpaRepository<UserCredentials, Long> {
    Optional<UserCredentials> findByEmail(String email);
    Optional<UserCredentials> findByUserId(Long userId);
    boolean existsByEmail(String login);
    boolean existsByUserId(Long userId);
}
