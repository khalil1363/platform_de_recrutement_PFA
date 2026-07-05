package com.example.auth.controller;

import com.example.auth.dto.AdminCreateUserRequest;
import com.example.auth.dto.AdminUpdateUserRequest;
import com.example.auth.dto.AuthenticationResponse;
import com.example.auth.dto.FileUploadResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.dto.UpdateProfileRequest;
import com.example.auth.dto.UpdateUserStatusRequest;
import com.example.auth.dto.UserResponse;
import com.example.auth.response.ApiResponse;
import com.example.auth.security.CustomUserDetails;
import com.example.auth.service.AuthenticationService;
import com.example.auth.service.FileStorageService;
import com.example.auth.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * REST controller for authentication and user profile endpoints.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationService authenticationService;
    private final UserService userService;
    private final FileStorageService fileStorageService;

    /**
     * Registers a new user account.
     *
     * @param request registration payload
     * @return authentication response with JWT token
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> register(
            @Valid @RequestBody RegisterRequest request) {
        AuthenticationResponse response = authenticationService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User registered successfully", response, HttpStatus.CREATED.value()));
    }

    /**
     * Authenticates a user and returns a JWT token.
     *
     * @param request login payload
     * @return authentication response with JWT token
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest request) {
        AuthenticationResponse response = authenticationService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Login successful", response, HttpStatus.OK.value()));
    }

    /**
     * Uploads a profile image and returns its public URL.
     *
     * @param file profile image file
     * @return uploaded file URL
     */
    @PostMapping("/upload/profile-image")
    public ResponseEntity<ApiResponse<FileUploadResponse>> uploadProfileImage(
            @RequestParam("file") MultipartFile file) {
        String profileImageUrl = fileStorageService.storeProfileImage(file);
        FileUploadResponse response = FileUploadResponse.builder()
                .profileImageUrl(profileImageUrl)
                .build();
        return ResponseEntity.ok(ApiResponse.success("Profile image uploaded", response, HttpStatus.OK.value()));
    }

    /**
     * Returns all registered users.
     *
     * @return list of user profiles
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<List<UserResponse>>> getAllUsers() {
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(ApiResponse.success("Users retrieved successfully", users, HttpStatus.OK.value()));
    }

    /**
     * Creates a new user account with a specific role (admin only).
     *
     * @param request admin create user payload
     * @return created user profile
     */
    @PostMapping("/users")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> createUser(
            @Valid @RequestBody AdminCreateUserRequest request) {
        UserResponse user = userService.createUserByAdmin(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("User created successfully", user, HttpStatus.CREATED.value()));
    }

    /**
     * Activates or deactivates a user account (admin only).
     *
     * @param userId the user identifier
     * @param request active status payload
     * @return updated user profile
     */
    @PatchMapping("/users/{userId}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUserStatus(
            @PathVariable String userId,
            @Valid @RequestBody UpdateUserStatusRequest request) {
        UserResponse user = userService.updateUserStatus(userId, request.getActive());
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", user, HttpStatus.OK.value()));
    }

    /**
     * Updates a user account (admin only).
     *
     * @param userId the user identifier
     * @param request admin update payload
     * @return updated user profile
     */
    @PutMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateUser(
            @PathVariable String userId,
            @Valid @RequestBody AdminUpdateUserRequest request) {
        UserResponse user = userService.updateUserByAdmin(userId, request);
        return ResponseEntity.ok(ApiResponse.success("User updated successfully", user, HttpStatus.OK.value()));
    }

    /**
     * Deletes a user account (admin only).
     *
     * @param userId the user identifier
     * @param userDetails the authenticated admin
     * @return empty success response
     */
    @DeleteMapping("/users/{userId}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable String userId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        userService.deleteUser(userId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User deleted successfully", null, HttpStatus.OK.value()));
    }

    /**
     * Returns the currently authenticated user's profile information.
     *
     * @param userDetails the authenticated user details
     * @return user profile response
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        UserResponse response = userService.getCurrentUser(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("User profile retrieved", response, HttpStatus.OK.value()));
    }

    /**
     * Updates the currently authenticated user's profile.
     *
     * @param userDetails the authenticated user details
     * @param request profile update payload
     * @return updated user profile
     */
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> updateCurrentUser(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        UserResponse response = userService.updateProfile(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.success("Profile updated successfully", response, HttpStatus.OK.value()));
    }
}
