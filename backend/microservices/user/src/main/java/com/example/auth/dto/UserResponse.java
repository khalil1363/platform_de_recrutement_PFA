package com.example.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Date;
import java.util.List;

/**
 * Response payload containing authenticated user profile information.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {

    private String userId;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private String address;
    private String profileImageUrl;
    private String meetingLink;
    private Date lastLoginDate;
    private Date joinDate;
    private String role;
    private List<String> authorities;
    private boolean active;
    private boolean notLocked;
}
