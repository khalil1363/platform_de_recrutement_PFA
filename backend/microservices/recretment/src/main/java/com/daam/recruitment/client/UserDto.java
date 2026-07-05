package com.daam.recruitment.client;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class UserDto {
    private String userId;
    private String firstName;
    private String lastName;
    private String username;
    private String email;
    private String phoneNumber;
    private String address;
    private String profileImageUrl;
    private String role;
    private boolean active;
}
