package com.goylik.auth_service.service;

import com.goylik.auth_service.model.dto.request.*;
import com.goylik.auth_service.model.dto.response.TokenResponse;
import com.goylik.auth_service.model.dto.response.TokenValidationResponse;

/**
 * Service interface for handling authentication and authorization operations.
 * Provides methods for user authentication, token management, and credential handling.
 */
public interface AuthService {

    /**
     * Authenticates a user with the provided credentials.
     *
     * @param request the login request containing email and password
     * @return a {@link TokenResponse} containing access and refresh tokens
     * @throws org.springframework.security.authentication.BadCredentialsException if credentials are invalid
     * @throws com.goylik.auth_service.exception.UserNotFoundException if user with given email is not found
     */
    TokenResponse login(LoginRequest request);

    /**
     * Refreshes an expired access token using a valid refresh token.
     * The old refresh token is revoked and a new pair of tokens is generated.
     *
     * @param request the refresh token request containing the refresh token
     * @return a new {@link TokenResponse} containing fresh access and refresh tokens
     * @throws com.goylik.auth_service.exception.InvalidTokenException if the refresh token is invalid, expired, or revoked
     */
    TokenResponse refreshToken(RefreshTokenRequest request);

    /**
     * Validates an access token and extracts its claims.
     *
     * @param request the validation request containing the token to validate
     * @return a {@link TokenValidationResponse} indicating whether the token is valid,
     *         along with the user ID and role if valid
     */
    TokenValidationResponse validateToken(ValidateTokenRequest request);

    /**
     * Saves user credentials when credentials are created by an internal service.
     * Encrypts the password before persisting and ensures email and userId uniqueness.
     *
     * @param request the credentials request containing user details (userId, email, password, role)
     * @throws com.goylik.auth_service.exception.EmailAlreadyExistsException if the email is already registered
     * @throws com.goylik.auth_service.exception.CredentialsAlreadyExistException if credentials for the userId already exist
     */
    void saveCredentials(SaveCredentialsRequest request);

    /**
     * Logs out a user by revoking the provided refresh token.
     * The token is marked as revoked and can no longer be used to refresh access tokens.
     *
     * @param request the logout request containing the refresh token to revoke
     */
    void logout(LogoutRequest request);
}