package com.example.auth.service;

import com.example.auth.dto.AuthenticationResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;

/**
 * Service contract for authentication operations.
 */
public interface AuthenticationService {

    /**
     * Registers a new user and returns a JWT token.
     *
     * @param request registration request payload
     * @return authentication response with JWT
     */
    AuthenticationResponse register(RegisterRequest request);

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request login request payload
     * @return authentication response with JWT
     */
    AuthenticationResponse login(LoginRequest request);
}
