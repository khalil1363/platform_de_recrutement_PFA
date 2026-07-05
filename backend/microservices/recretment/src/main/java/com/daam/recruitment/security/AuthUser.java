package com.daam.recruitment.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Collection;
import java.util.List;

@Getter
public class AuthUser implements UserDetails {
    private final String userId;
    private final String username;
    private final String role;
    private final boolean active;

    public AuthUser(String userId, String username, String role, boolean active) {
        this.userId = userId;
        this.username = username;
        this.role = role;
        this.active = active;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role));
    }

    @Override public String getPassword() { return ""; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return active; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return active; }

    public boolean isAdmin() { return "ROLE_ADMIN".equals(role); }
    public boolean isRh() { return "ROLE_RH".equals(role); }
    public boolean isCandidate() { return "ROLE_USER".equals(role); }
}
