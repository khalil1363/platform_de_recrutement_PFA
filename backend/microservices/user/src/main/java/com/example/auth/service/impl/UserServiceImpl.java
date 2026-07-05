package com.example.auth.service.impl;

import com.example.auth.dto.AdminCreateUserRequest;
import com.example.auth.dto.AdminUpdateUserRequest;
import com.example.auth.dto.UpdateProfileRequest;
import com.example.auth.dto.UserResponse;
import com.example.auth.entity.User;
import com.example.auth.enumeration.Role;
import com.example.auth.exception.EmailAlreadyExistsException;
import com.example.auth.exception.InvalidRoleException;
import com.example.auth.exception.OperationNotAllowedException;
import com.example.auth.exception.UserNotFoundException;
import com.example.auth.exception.UsernameAlreadyExistsException;
import com.example.auth.repository.UserRepository;
import com.example.auth.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of user-related operations.
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String username) {
        return getUserByUsername(username);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
        return mapToUserResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(String userId) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        return mapToUserResponse(user);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse createUserByAdmin(AdminCreateUserRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameAlreadyExistsException("Username already exists");
        }
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        String roleAuthority = resolveRoleAuthority(request.getRole());
        Date now = new Date();

        User user = User.builder()
                .userId(UUID.randomUUID().toString())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phoneNumber(request.getPhoneNumber())
                .address(request.getAddress())
                .profileImageUrl(request.getProfileImageUrl())
                .joinDate(now)
                .role(roleAuthority)
                .authorities(resolveAuthoritiesForRole(roleAuthority))
                .isActive(true)
                .isNotLocked(true)
                .build();

        return mapToUserResponse(userRepository.save(user));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse updateUserStatus(String userId, boolean active) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));
        user.setActive(active);
        return mapToUserResponse(userRepository.save(user));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse updateProfile(String username, UpdateProfileRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));

        if (userRepository.existsByEmailAndUserIdNot(request.getEmail(), user.getUserId())) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        applyProfileFields(user, request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getPhoneNumber(), request.getAddress(), request.getProfileImageUrl());

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return mapToUserResponse(userRepository.save(user));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserResponse updateUserByAdmin(String userId, AdminUpdateUserRequest request) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (userRepository.existsByEmailAndUserIdNot(request.getEmail(), userId)) {
            throw new EmailAlreadyExistsException("Email already exists");
        }

        String roleAuthority = resolveRoleAuthority(request.getRole());
        applyProfileFields(user, request.getFirstName(), request.getLastName(), request.getEmail(),
                request.getPhoneNumber(), request.getAddress(), request.getProfileImageUrl());

        user.setRole(roleAuthority);
        user.setAuthorities(resolveAuthoritiesForRole(roleAuthority));

        if (StringUtils.hasText(request.getPassword())) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return mapToUserResponse(userRepository.save(user));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public void deleteUser(String userId, String requestingUsername) {
        User user = userRepository.findByUserId(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with id: " + userId));

        if (user.getUsername().equals(requestingUsername)) {
            throw new OperationNotAllowedException("You cannot delete your own account");
        }

        userRepository.delete(user);
    }

    private void applyProfileFields(User user, String firstName, String lastName, String email,
                                    String phoneNumber, String address, String profileImageUrl) {
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setEmail(email);
        user.setPhoneNumber(phoneNumber);
        user.setAddress(address);
        user.setProfileImageUrl(profileImageUrl);
    }

    private String resolveRoleAuthority(String role) {
        return Arrays.stream(Role.values())
                .filter(r -> r.getAuthority().equalsIgnoreCase(role) || r.name().equalsIgnoreCase(role))
                .findFirst()
                .map(Role::getAuthority)
                .orElseThrow(() -> new InvalidRoleException("Invalid role: " + role));
    }

    private List<String> resolveAuthoritiesForRole(String roleAuthority) {
        if (Role.ADMIN.getAuthority().equals(roleAuthority)) {
            return List.of("read", "edit", "delete");
        }
        if (Role.RH.getAuthority().equals(roleAuthority)) {
            return List.of("read", "edit", "manage_candidates");
        }
        if (Role.DEVELOPER.getAuthority().equals(roleAuthority)) {
            return List.of("read", "edit", "manage_system");
        }
        return List.of("read", "edit");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .address(user.getAddress())
                .profileImageUrl(user.getProfileImageUrl())
                .lastLoginDate(user.getLastLoginDate())
                .joinDate(user.getJoinDate())
                .role(user.getRole())
                .authorities(user.getAuthorities())
                .active(user.isActive())
                .notLocked(user.isNotLocked())
                .build();
    }
}
