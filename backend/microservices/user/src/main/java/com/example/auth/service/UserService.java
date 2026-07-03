package com.example.auth.service;

import com.example.auth.dto.UserResponse;
import com.example.auth.entity.User;

import java.util.List;

/**
 * Service contract for user-related operations.
 */
public interface UserService {

    /**
     * Retrieves all registered users.
     *
     * @return list of user profile responses
     */
    List<UserResponse> getAllUsers();

    /**
     * Retrieves the currently authenticated user's profile.
     *
     * @param username the authenticated username
     * @return user profile response
     */
    UserResponse getCurrentUser(String username);

    /**
     * Maps a User entity to a UserResponse DTO.
     *
     * @param user the user entity
     * @return user response DTO without sensitive data
     */
    UserResponse mapToUserResponse(User user);
}
