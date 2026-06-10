package com.shiftleft.hub.common.config;

import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.tag.domain.Tag;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.ticket.domain.Ticket;
import com.shiftleft.hub.ticket.domain.TicketRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class DataSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AiConfigRepository aiConfigRepository;
    private final WorkspaceService workspaceService;
    private final ArticleRepository articleRepository;
    private final TicketRepository ticketRepository;
    private final TagRepository tagRepository;

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    /**
     * Seed admin user, regular user, tech agent, and default AI config.
     * Runs after the application context is fully initialized (Flyway + JPA ready).
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
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

    /**
     * Migrates all existing v1.0 data into a Default Workspace on startup.
     * Runs after the seed() method to ensure users exist before assignment.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(2)
    public void migrateWorkspaces() {
        if (workspaceService.findBySlug("default").isEmpty()) {
            log.info("Starting v1.0 -> Default Workspace migration...");
            migrateToDefaultWorkspace();
            log.info("Default Workspace migration complete");
        } else {
            log.info("Default Workspace already exists - migration already completed");
        }
    }

    private void migrateToDefaultWorkspace() {
        User admin = userRepository.findAll().stream()
            .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
            .findFirst()
            .orElse(null);
        UUID creatorId = admin != null ? admin.getId() : UUID.randomUUID();
        Workspace defaultWorkspace = workspaceService.createWorkspace(
            "Default Workspace", "Default workspace for existing v1.0 data", null, creatorId);
        log.info("Created Default Workspace (slug: default)");

        UUID defaultWsId = defaultWorkspace.getId();

        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (!workspaceService.isMemberOfWorkspace(defaultWsId, user.getId())) {
                workspaceService.assignUserToWorkspace(defaultWsId, user.getId(), "ADMIN");
            }
            if (user.getDefaultWorkspaceId() == null) {
                user.setDefaultWorkspaceId(defaultWsId);
                userRepository.save(user);
            }
        }
        log.info("Migrated {} users to Default Workspace", allUsers.size());

        List<Article> allArticles = articleRepository.findAll();
        for (Article article : allArticles) {
            if (article.getWorkspaceId() == null) {
                article.setWorkspaceId(defaultWsId);
            }
        }
        if (!allArticles.isEmpty()) {
            articleRepository.saveAll(allArticles);
        }

        List<Ticket> allTickets = ticketRepository.findAll();
        for (Ticket ticket : allTickets) {
            if (ticket.getWorkspaceId() == null) {
                ticket.setWorkspaceId(defaultWsId);
            }
        }
        if (!allTickets.isEmpty()) {
            ticketRepository.saveAll(allTickets);
        }

        List<Tag> allTags = tagRepository.findAll();
        for (Tag tag : allTags) {
            if (tag.getWorkspaceId() == null) {
                tag.setWorkspaceId(defaultWsId);
            }
        }
        if (!allTags.isEmpty()) {
            tagRepository.saveAll(allTags);
        }

        log.info("Default Workspace migration complete - assigned {} articles, {} tickets, {} tags",
            allArticles.size(), allTickets.size(), allTags.size());
    }
}
