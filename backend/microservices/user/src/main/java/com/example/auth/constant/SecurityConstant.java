package com.example.auth.constant;

/**
 * Security-related URL and configuration constants.
 */
public final class SecurityConstant {

    public static final String AUTH_BASE_PATH = "/api/auth";
    public static final String LOGIN_PATH = AUTH_BASE_PATH + "/login";
    public static final String REGISTER_PATH = AUTH_BASE_PATH + "/register";
    public static final String UPLOAD_PROFILE_IMAGE_PATH = AUTH_BASE_PATH + "/upload/profile-image";
    public static final String FILES_PATH = AUTH_BASE_PATH + "/files/**";
    public static final String INTERNAL_PATH = AUTH_BASE_PATH + "/internal/**";

    public static final String[] PUBLIC_ENDPOINTS = {
            LOGIN_PATH,
            REGISTER_PATH,
            UPLOAD_PROFILE_IMAGE_PATH,
            FILES_PATH,
            INTERNAL_PATH
    };

    private SecurityConstant() {
    }
}
