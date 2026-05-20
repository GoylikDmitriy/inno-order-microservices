package com.goylik.auth_service.controller;

import com.goylik.auth_service.model.dto.request.*;
import com.goylik.auth_service.model.dto.response.TokenResponse;
import com.goylik.auth_service.model.dto.response.TokenValidationResponse;
import com.goylik.auth_service.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for authentication operations.
 * Provides endpoints for user authentication, token management, and credential handling.
 * All endpoints are accessible under the base path {@code /api/auth}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    /**
     * Saves user credentials when credentials are created by an internal service.
     * This endpoint is protected by an internal API key and should only be called by trusted services.
     *
     * @param request the credentials request containing user details (userId, email, password, role)
     * @return HTTP 201 (Created) if credentials are successfully saved
     * @throws com.goylik.auth_service.exception.EmailAlreadyExistsException if the email is already registered
     * @throws com.goylik.auth_service.exception.CredentialsAlreadyExistException if credentials for the userId already exist
     */
    @PostMapping("/save-credentials")
    @ResponseStatus(HttpStatus.CREATED)
    public void saveCredentials(@Valid @RequestBody SaveCredentialsRequest request) {
        authService.saveCredentials(request);
    }

    /**
     * Authenticates a user with their email and password.
     *
     * @param request the login request containing email and password
     * @return a {@link TokenResponse} containing access and refresh tokens
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    /**
     * Validates an access token and returns its claims.
     *
     * @param request the validation request containing the token to validate
     * @return a {@link TokenValidationResponse} indicating whether the token is valid,
     *         along with the user ID and role if valid
     */
    @PostMapping("/validate")
    public TokenValidationResponse validateToken(@Valid @RequestBody ValidateTokenRequest request) {
        return authService.validateToken(request);
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * The old refresh token is revoked and a new pair of tokens is generated.
     *
     * @param request the refresh token request containing the refresh token
     * @return a new {@link TokenResponse} containing fresh access and refresh tokens
     * @throws com.goylik.auth_service.exception.InvalidTokenException if the refresh token is invalid, expired, or revoked
     */
    @PostMapping("/refresh")
    public TokenResponse refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        return authService.refreshToken(request);
    }

    /**
     * Logs out a user by revoking the provided refresh token.
     * Once revoked, the token can no longer be used to refresh access tokens.
     *
     * @param request the logout request containing the refresh token to revoke
     * @return HTTP 204 (No Content) if logout is successful
     */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@Valid @RequestBody LogoutRequest request) {
        authService.logout(request);
    }
}
