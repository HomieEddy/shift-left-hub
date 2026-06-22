package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.List;

/**
 * Seeds the admin user and the 6 non-admin department users.
 *
 * <p>Single responsibility: idempotent user creation with secure random
 * passwords for non-admin accounts. Returns the created or existing admin
 * user so the orchestrator can pass it to downstream seeders.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserSeeder {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SEED_PASSWORD_ALPHABET =
        "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private static final List<UserSeed> NON_ADMIN_USERS = List.of(
        new UserSeed("hr.user@company.com", "HR User", UserRole.ROLE_USER),
        new UserSeed("hr.tech@company.com", "HR Tech", UserRole.ROLE_AGENT),
        new UserSeed("legal.user@company.com", "Legal User", UserRole.ROLE_USER),
        new UserSeed("legal.tech@company.com", "Legal Tech", UserRole.ROLE_AGENT),
        new UserSeed("it.user@company.com", "IT User", UserRole.ROLE_USER),
        new UserSeed("it.tech@company.com", "IT Tech", UserRole.ROLE_AGENT)
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Seeds all users. Creates the admin (using {@code adminEmail} + {@code adminPassword})
     * if not present, then the 6 department users with random 24-char passwords.
     *
     * @param adminEmail    the configured admin email
     * @param adminPassword the configured admin password (plaintext; encoded before save)
     * @return the admin User entity (existing or newly created)
     */
    public User seedUsers(String adminEmail, String adminPassword) {
        User admin = seedAdmin(adminEmail, adminPassword);
        for (UserSeed us : NON_ADMIN_USERS) {
            seedNonAdmin(us);
        }
        return admin;
    }

    private User seedAdmin(String adminEmail, String adminPassword) {
        if (userRepository.existsByEmail(adminEmail)) {
            return userRepository.findByEmail(adminEmail).orElseThrow();
        }
        User admin = User.builder()
            .email(adminEmail)
            .password(passwordEncoder.encode(adminPassword))
            .displayName("System Admin")
            .role(UserRole.ROLE_ADMIN)
            .enabled(true)
            .build();
        userRepository.save(admin);
        log.info("Created admin seed user: {}", adminEmail);
        return admin;
    }

    private void seedNonAdmin(UserSeed us) {
        if (userRepository.existsByEmail(us.email())) {
            log.debug("Seed user {} already exists - skipping", us.email());
            return;
        }
        String seedPassword = generateSeedPassword();
        User user = User.builder()
            .email(us.email())
            .password(passwordEncoder.encode(seedPassword))
            .displayName(us.displayName())
            .role(us.role())
            .enabled(true)
            .build();
        userRepository.save(user);
        log.info("Created seed user: {} ({}) - role: {} - initial password: {}",
            us.email(), us.displayName(), us.role(), seedPassword);
    }

    /**
     * Generates a 24-char password using a high-entropy alphabet
     * (ambiguous chars 0/O/1/l/I removed).
     *
     * @return the random password
     */
    public static String generateSeedPassword() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(SEED_PASSWORD_ALPHABET.charAt(RANDOM.nextInt(SEED_PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    private record UserSeed(String email, String displayName, UserRole role) {
    }
}
