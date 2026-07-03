package com.example.auth.constant;

/**
 * Security-related URL and configuration constants.
 */
public final class SecurityConstant {

    public static final String AUTH_BASE_PATH = "/api/auth";
    public static final String LOGIN_PATH = AUTH_BASE_PATH + "/login";
    public static final String REGISTER_PATH = AUTH_BASE_PATH + "/register";

    public static final String[] PUBLIC_ENDPOINTS = {
            LOGIN_PATH,
            REGISTER_PATH
    };

    private SecurityConstant() {
    }
}
