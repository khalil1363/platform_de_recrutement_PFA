package com.example.auth.enumeration;

/**
 * Application roles with their Spring Security authority representation.
 */
public enum Role {

    ADMIN("ROLE_ADMIN"),
    RH("ROLE_RH"),
    DEVELOPER("ROLE_DEVELOPER"),
    USER("ROLE_USER");

    private final String authority;

    Role(String authority) {
        this.authority = authority;
    }

    public String getAuthority() {
        return authority;
    }
}
