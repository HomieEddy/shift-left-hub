package com.shiftleft.hub.common.config;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail("admin@shiftleft.com")) {
            User admin = User.builder()
                .email("admin@shiftleft.com")
                .password(passwordEncoder.encode("Admin123!"))
                .displayName("System Admin")
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .build();
            userRepository.save(admin);
            log.info("Created default admin user with email: admin@shiftleft.com");
            log.warn("Default admin password is 'Admin123!'. Change it on first login for security.");
        }
    }
}
