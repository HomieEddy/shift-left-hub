package com.shiftleft.hub.common.config;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (adminEmail == null || adminPassword == null) {
            log.info("Admin seeder skipped — set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD to seed an admin user");
            return;
        }
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .displayName("System Admin")
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .build();
            userRepository.save(admin);
            log.info("Created default admin user with email: {}", adminEmail);
            log.warn("Change the default admin password on first login for security.");
        }
    }
}
