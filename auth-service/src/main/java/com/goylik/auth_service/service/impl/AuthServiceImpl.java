package com.goylik.auth_service.service.impl;

import com.goylik.auth_service.exception.CredentialsAlreadyExistException;
import com.goylik.auth_service.exception.EmailAlreadyExistsException;
import com.goylik.auth_service.exception.InvalidTokenException;
import com.goylik.auth_service.exception.UserNotFoundException;
import com.goylik.auth_service.model.dto.request.*;
import com.goylik.auth_service.model.dto.response.TokenResponse;
import com.goylik.auth_service.model.dto.response.TokenValidationResponse;
import com.goylik.auth_service.model.entity.RefreshToken;
import com.goylik.auth_service.model.entity.UserCredentials;
import com.goylik.auth_service.model.enums.Role;
import com.goylik.auth_service.repository.RefreshTokenRepository;
import com.goylik.auth_service.repository.UserCredentialsRepository;
import com.goylik.auth_service.security.jwt.JwtService;
import com.goylik.auth_service.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UserCredentialsRepository userCredentialsRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    @Override
    @Transactional
    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        var credentials = fetchUserCredentialsByEmailOrThrow(request.email());

        return buildTokenResponseWithRefresh(credentials.getUserId(), credentials.getRole());
    }

    private UserCredentials fetchUserCredentialsByEmailOrThrow(String email) {
        return userCredentialsRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not registered with email: " + email));
    }

    private TokenResponse buildTokenResponseWithRefresh(Long userId, Role role) {
        refreshTokenRepository.revokeAllByUserId(userId);

        String accessToken = jwtService.generateAccessToken(userId, role);
        String refreshToken = jwtService.generateRefreshToken(userId, role);

        buildRefreshTokenAndSave(userId, refreshToken);

        return new TokenResponse(accessToken, refreshToken);
    }

    private void buildRefreshTokenAndSave(Long userId, String token) {
        var tokenEntity = new RefreshToken();
        tokenEntity.setToken(token);
        tokenEntity.setUserId(userId);
        tokenEntity.setExpiresAt(LocalDateTime.now().plus(jwtService.getRefreshTokenExpirationMs(), ChronoUnit.MILLIS));
        tokenEntity.setRevoked(false);
        refreshTokenRepository.save(tokenEntity);
    }

    @Override
    public TokenResponse refreshToken(RefreshTokenRequest request) {
        var tokenResponse = jwtService.extractAllFromRefreshToken(request.refreshToken());

        validateRefreshTokenOrThrow(request.refreshToken());

        return buildTokenResponseWithRefresh(tokenResponse.userId(), tokenResponse.role());
    }

    private void validateRefreshTokenOrThrow(String token) {
        var storedToken = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> new InvalidTokenException("Refresh token not found"));

        if (storedToken.isRevoked()) {
            throw new InvalidTokenException("Token revoked");
        }

        if (storedToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new InvalidTokenException("Token expired");
        }
    }

    @Override
    public TokenValidationResponse validateToken(ValidateTokenRequest request) {
        return jwtService.extractAllFromAccessToken(request.token());
    }

    @Override
    @Transactional
    public void saveCredentials(SaveCredentialsRequest request) {
        if (userCredentialsRepository.existsByEmail(request.email())) {
            throw new EmailAlreadyExistsException("Email already taken: " + request.email());
        }

        if (userCredentialsRepository.existsByUserId(request.userId())) {
            throw new CredentialsAlreadyExistException("Credentials already exist for userId: " + request.userId());
        }

        var credentials = buildUserCredentials(request);
        userCredentialsRepository.save(credentials);
    }

    private UserCredentials buildUserCredentials(SaveCredentialsRequest request) {
        var credentials = new UserCredentials();
        credentials.setUserId(request.userId());
        credentials.setEmail(request.email());
        credentials.setPassword(passwordEncoder.encode(request.password()));
        credentials.setRole(request.role());
        credentials.setActive(true);

        return credentials;
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        Long userId = jwtService.extractUserId(request.refreshToken());
        validateRefreshTokenOrThrow(request.refreshToken());
        refreshTokenRepository.revokeAllByUserId(userId);
    }
}
