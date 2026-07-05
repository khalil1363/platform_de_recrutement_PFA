package com.daam.recruitment.client;

import com.daam.recruitment.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "USER")
public interface UserClient {

    @GetMapping("/api/auth/internal/users/username/{username}")
    ApiResponse<UserDto> getUserByUsername(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable("username") String username);

    @GetMapping("/api/auth/internal/users/{userId}")
    ApiResponse<UserDto> getUserById(
            @RequestHeader("X-Internal-Key") String internalKey,
            @PathVariable("userId") String userId);
}
