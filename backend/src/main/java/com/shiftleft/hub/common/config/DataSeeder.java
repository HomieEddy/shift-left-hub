package com.shiftleft.hub.common.config;

import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AiConfigRepository aiConfigRepository;

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    /**
     * Seed admin user, regular user, tech agent, and default AI config.
     * Runs after the application context is fully initialized (Flyway + JPA ready).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void seed() {
        if (adminEmail == null || adminPassword == null) {
            log.info("Admin seeder skipped — set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD to seed admin user");
        } else {
            seedUser(adminEmail, "System Admin", UserRole.ROLE_ADMIN);
            seedUser(deriveEmail("user"), "Regular User", UserRole.ROLE_USER);
            seedUser(deriveEmail("tech"), "Tech Agent", UserRole.ROLE_AGENT);
        }
        seedAiConfig();
    }

    private void seedUser(String email, String displayName, UserRole role) {
        if (!userRepository.existsByEmail(email)) {
            userRepository.save(User.builder()
                .email(email)
                .password(passwordEncoder.encode(adminPassword))
                .displayName(displayName)
                .role(role)
                .enabled(true)
                .build()
            );
            log.info("Created {} user with email: {}", role, email);
        } else {
            log.info("{} user {} already exists — skipping", role, email);
        }
    }

    private String deriveEmail(String prefix) {
        int at = adminEmail.indexOf('@');
        if (at == -1) {
            return prefix + "@shiftleft.com";
        }
        return prefix + adminEmail.substring(at);
    }

    private void seedAiConfig() {
        if (aiConfigRepository.count() == 0) {
            AiConfig config = AiConfig.builder()
                .llmProvider("OLLAMA")
                .ollamaEndpointUrl("http://host.docker.internal:11434")
                .openaiApiKey(null)
                .chatModelName("llama3.2:3b")
                .embeddingModelName("nomic-embed-text")
                .similarityThreshold(0.7)
                .embeddingDimension(768)
                .build();
            aiConfigRepository.save(config);
            log.info("Created default AI config (Ollama local)");
        }
    }
}
