package com.goylik.auth_service.repository;

import com.goylik.auth_service.model.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenAndRevokedFalse(String token);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true WHERE t.userId = :userId AND t.revoked = false")
    void revokeAllByUserId(Long userId);

    boolean existsByUserIdAndRevokedFalse(Long userId);
}
