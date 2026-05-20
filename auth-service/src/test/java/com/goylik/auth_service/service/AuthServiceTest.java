package com.goylik.auth_service.service;

import com.goylik.auth_service.exception.CredentialsAlreadyExistException;
import com.goylik.auth_service.exception.EmailAlreadyExistsException;
import com.goylik.auth_service.exception.InvalidTokenException;
import com.goylik.auth_service.exception.UserNotFoundException;
import com.goylik.auth_service.model.dto.request.LoginRequest;
import com.goylik.auth_service.model.dto.request.RefreshTokenRequest;
import com.goylik.auth_service.model.dto.request.SaveCredentialsRequest;
import com.goylik.auth_service.model.dto.request.ValidateTokenRequest;
import com.goylik.auth_service.model.dto.response.TokenResponse;
import com.goylik.auth_service.model.dto.response.TokenValidationResponse;
import com.goylik.auth_service.model.entity.RefreshToken;
import com.goylik.auth_service.model.entity.UserCredentials;
import com.goylik.auth_service.model.enums.Role;
import com.goylik.auth_service.repository.RefreshTokenRepository;
import com.goylik.auth_service.repository.UserCredentialsRepository;
import com.goylik.auth_service.security.jwt.JwtService;
import com.goylik.auth_service.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserCredentialsRepository userCredentialsRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;

    @InjectMocks private AuthServiceImpl authService;

    private UserCredentials credentials;
    private RefreshToken refreshToken;

    @BeforeEach
    void setUp() {
        credentials = new UserCredentials();
        credentials.setId(1L);
        credentials.setUserId(10L);
        credentials.setEmail("john@test.com");
        credentials.setPassword("hashed_password");
        credentials.setRole(Role.ROLE_USER);
        credentials.setActive(true);

        refreshToken = new RefreshToken();
        refreshToken.setToken("valid_refresh_token");
        refreshToken.setId(1L);
        refreshToken.setRevoked(false);
        refreshToken.setExpiresAt(LocalDateTime.MAX);
        refreshToken.setUserId(1L);
    }

    @Test
    void login_shouldReturnTokens_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest("john@test.com", "password");

        when(userCredentialsRepository.findByEmail("john@test.com"))
                .thenReturn(Optional.of(credentials));
        when(jwtService.generateAccessToken(10L, Role.ROLE_USER))
                .thenReturn("access_token");
        when(jwtService.generateRefreshToken(10L, Role.ROLE_USER))
                .thenReturn("refresh_token");

        TokenResponse result = authService.login(request);

        assertNotNull(result);
        assertEquals("access_token", result.accessToken());
        assertEquals("refresh_token", result.refreshToken());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void login_shouldThrowBadCredentials_WhenPasswordIsWrong() {
        LoginRequest request = new LoginRequest("john@test.com", "wrong_password");

        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any());

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(request)
        );

        verify(userCredentialsRepository, never()).findByEmail(any());
    }

    @Test
    void login_shouldThrowUserNotFoundException_WhenEmailNotFound() {
        LoginRequest request = new LoginRequest("unknown@test.com", "password");

        when(userCredentialsRepository.findByEmail("unknown@test.com"))
                .thenReturn(Optional.empty());

        assertThrows(
                UserNotFoundException.class,
                () -> authService.login(request)
        );
    }

    @Test
    void login_shouldThrowDisabledException_WhenAccountIsDisabled() {
        LoginRequest request = new LoginRequest("john@test.com", "password");

        doThrow(new DisabledException("Account disabled"))
                .when(authenticationManager).authenticate(any());

        assertThrows(
                DisabledException.class,
                () -> authService.login(request)
        );

        verify(userCredentialsRepository, never()).findByEmail(any());
    }

    @Test
    void refreshToken_shouldReturnNewTokens_WhenRefreshTokenIsValid() {
        RefreshTokenRequest request = new RefreshTokenRequest("valid_refresh_token");

        when(jwtService.extractAllFromRefreshToken("valid_refresh_token"))
                .thenReturn(new TokenValidationResponse(true, 10L, Role.ROLE_USER));
        when(refreshTokenRepository.findByTokenAndRevokedFalse("valid_refresh_token"))
                .thenReturn(Optional.of(refreshToken));
        when(jwtService.generateAccessToken(10L, Role.ROLE_USER))
                .thenReturn("new_access_token");
        when(jwtService.generateRefreshToken(10L, Role.ROLE_USER))
                .thenReturn("new_refresh_token");

        TokenResponse result = authService.refreshToken(request);

        assertNotNull(result);
        assertEquals("new_access_token", result.accessToken());
        assertEquals("new_refresh_token", result.refreshToken());
    }

    @Test
    void refreshToken_shouldThrowInvalidTokenException_WhenTokenIsExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest("expired_token");

        when(jwtService.extractAllFromRefreshToken("expired_token"))
                .thenReturn(new TokenValidationResponse(false, null, null));

        assertThrows(
                InvalidTokenException.class,
                () -> authService.refreshToken(request)
        );

        verify(jwtService, never()).generateAccessToken(any(), any());
        verify(jwtService, never()).generateRefreshToken(any(), any());
    }

    @Test
    void validateToken_shouldReturnValidResponse_WhenTokenIsValid() {
        ValidateTokenRequest request = new ValidateTokenRequest("valid_token");

        when(jwtService.extractAllFromAccessToken("valid_token"))
                .thenReturn(new TokenValidationResponse(true, 10L, Role.ROLE_USER));

        TokenValidationResponse result = authService.validateToken(request);

        assertTrue(result.valid());
        assertEquals(10L, result.userId());
        assertEquals(Role.ROLE_USER, result.role());
    }

    @Test
    void validateToken_shouldReturnInvalidResponse_WhenTokenIsInvalid() {
        ValidateTokenRequest request = new ValidateTokenRequest("invalid_token");

        when(jwtService.extractAllFromAccessToken("invalid_token"))
                .thenReturn(new TokenValidationResponse(false, null, null));

        TokenValidationResponse result = authService.validateToken(request);

        assertFalse(result.valid());
        assertNull(result.userId());
        assertNull(result.role());
    }

    @Test
    void saveCredentials_shouldSaveSuccessfully_WhenValidRequest() {
        SaveCredentialsRequest request = new SaveCredentialsRequest(
                10L, "john@test.com", "password", Role.ROLE_USER
        );

        when(userCredentialsRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(userCredentialsRepository.existsByUserId(10L)).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hashed_password");

        authService.saveCredentials(request);

        verify(userCredentialsRepository).save(argThat(saved ->
                saved.getUserId().equals(10L) &&
                        saved.getEmail().equals("john@test.com") &&
                        saved.getPassword().equals("hashed_password") &&
                        saved.getRole().equals(Role.ROLE_USER) &&
                        saved.getActive()
        ));
    }

    @Test
    void saveCredentials_shouldThrowEmailAlreadyExists_WhenEmailIsTaken() {
        SaveCredentialsRequest request = new SaveCredentialsRequest(
                10L, "john@test.com", "password", Role.ROLE_USER
        );

        when(userCredentialsRepository.existsByEmail("john@test.com")).thenReturn(true);

        assertThrows(
                EmailAlreadyExistsException.class,
                () -> authService.saveCredentials(request)
        );

        verify(userCredentialsRepository, never()).save(any());
    }

    @Test
    void saveCredentials_shouldThrowCredentialsAlreadyExist_WhenUserIdAlreadyHasCredentials() {
        SaveCredentialsRequest request = new SaveCredentialsRequest(
                10L, "john@test.com", "password", Role.ROLE_USER
        );

        when(userCredentialsRepository.existsByEmail("john@test.com")).thenReturn(false);
        when(userCredentialsRepository.existsByUserId(10L)).thenReturn(true);

        assertThrows(
                CredentialsAlreadyExistException.class,
                () -> authService.saveCredentials(request)
        );

        verify(userCredentialsRepository, never()).save(any());
    }

    @Test
    void saveCredentials_shouldHashPassword_BeforeSaving() {
        SaveCredentialsRequest request = new SaveCredentialsRequest(
                10L, "john@test.com", "plain_password", Role.ROLE_USER
        );

        when(userCredentialsRepository.existsByEmail(any())).thenReturn(false);
        when(userCredentialsRepository.existsByUserId(any())).thenReturn(false);
        when(passwordEncoder.encode("plain_password")).thenReturn("hashed_password");

        authService.saveCredentials(request);

        verify(passwordEncoder).encode("plain_password");
        verify(userCredentialsRepository).save(argThat(saved ->
                saved.getPassword().equals("hashed_password")
        ));
    }
}
