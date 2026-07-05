package com.example.auth.service;

import com.example.auth.dto.AdminCreateUserRequest;
import com.example.auth.dto.AdminUpdateUserRequest;
import com.example.auth.dto.UpdateProfileRequest;
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
     * Retrieves a user by username for internal service-to-service calls.
     */
    UserResponse getUserByUsername(String username);

    /**
     * Retrieves a user by id for internal service-to-service calls.
     */
    UserResponse getUserById(String userId);

    /**
     * Creates a new user account with a specific role (admin only).
     *
     * @param request admin create user payload
     * @return created user profile response
     */
    UserResponse createUserByAdmin(AdminCreateUserRequest request);

    /**
     * Updates the active status of a user account.
     *
     * @param userId the user identifier
     * @param active whether the account should be active
     * @return updated user profile response
     */
    UserResponse updateUserStatus(String userId, boolean active);

    /**
     * Updates the authenticated user's profile.
     *
     * @param username the authenticated username
     * @param request profile update payload
     * @return updated user profile response
     */
    UserResponse updateProfile(String username, UpdateProfileRequest request);

    /**
     * Updates a user account (admin only).
     *
     * @param userId the user identifier
     * @param request admin update payload
     * @return updated user profile response
     */
    UserResponse updateUserByAdmin(String userId, AdminUpdateUserRequest request);

    /**
     * Deletes a user account (admin only).
     *
     * @param userId the user identifier to delete
     * @param requestingUsername the admin performing the deletion
     */
    void deleteUser(String userId, String requestingUsername);

    /**
     * Maps a User entity to a UserResponse DTO.
     *
     * @param user the user entity
     * @return user response DTO without sensitive data
     */
    UserResponse mapToUserResponse(User user);
}
