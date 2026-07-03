package com.example.auth.config;

import com.example.auth.entity.User;
import com.example.auth.enumeration.Role;
import com.example.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Seeds a default admin account for local development when enabled.
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.seed-admin", havingValue = "true")
public class AdminUserInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.username}")
    private String adminUsername;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.existsByUsername(adminUsername)) {
            return;
        }

        Date now = new Date();
        User admin = User.builder()
                .userId(UUID.randomUUID().toString())
                .firstName("System")
                .lastName("Admin")
                .username(adminUsername)
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .joinDate(now)
                .role(Role.ADMIN.getAuthority())
                .authorities(List.of("read", "edit", "delete"))
                .isActive(true)
                .isNotLocked(true)
                .build();

        userRepository.save(admin);
    }
}
