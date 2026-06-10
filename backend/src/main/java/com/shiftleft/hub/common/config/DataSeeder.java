package com.shiftleft.hub.common.config;

import com.shiftleft.hub.agent.domain.WorkNote;
import com.shiftleft.hub.agent.domain.WorkNoteRepository;
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
    private final WorkNoteRepository workNoteRepository;

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

    private static final String PUBLIC_WS_NAME = "public";
    private static final String PUBLIC_WS_SLUG = "public";

    /**
     * Creates the public workspace and assigns all existing data to it.
     * Runs at Order(3) to ensure seed users (Order 1) and KB articles (Order 2) exist first.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(3)
    public void migrateWorkspaces() {
        if (workspaceService.findBySlug(PUBLIC_WS_SLUG).isEmpty()) {
            log.info("Starting v1.0 -> public workspace migration...");
            migrateToPublicWorkspace();
        } else {
            log.info("Public workspace already exists — migration already completed");
        }
    }

    private void migrateToPublicWorkspace() {
        User admin = userRepository.findAll().stream()
            .filter(u -> u.getRole() == UserRole.ROLE_ADMIN)
            .findFirst()
            .orElse(null);
        UUID creatorId = admin != null ? admin.getId() : UUID.randomUUID();
        Workspace publicWs = workspaceService.createWorkspace(
            PUBLIC_WS_NAME, "Public workspace — all users have access by default", null, creatorId);
        log.info("Created public workspace (slug: {})", PUBLIC_WS_SLUG);

        UUID wsId = publicWs.getId();

        List<User> allUsers = userRepository.findAll();
        for (User user : allUsers) {
            if (!workspaceService.isMemberOfWorkspace(wsId, user.getId())) {
                workspaceService.assignUserToWorkspace(wsId, user.getId(), "ADMIN");
            }
            if (user.getDefaultWorkspaceId() == null) {
                user.setDefaultWorkspaceId(wsId);
                userRepository.save(user);
            }
        }
        log.info("Assigned {} users to public workspace", allUsers.size());

        List<Article> allArticles = articleRepository.findAll();
        for (Article article : allArticles) {
            if (article.getWorkspaceId() == null) {
                article.setWorkspaceId(wsId);
            }
        }
        if (!allArticles.isEmpty()) {
            articleRepository.saveAll(allArticles);
        }

        List<Ticket> allTickets = ticketRepository.findAll();
        for (Ticket ticket : allTickets) {
            if (ticket.getWorkspaceId() == null) {
                ticket.setWorkspaceId(wsId);
            }
        }
        if (!allTickets.isEmpty()) {
            ticketRepository.saveAll(allTickets);
        }

        List<Tag> allTags = tagRepository.findAll();
        for (Tag tag : allTags) {
            if (tag.getWorkspaceId() == null) {
                tag.setWorkspaceId(wsId);
            }
        }
        if (!allTags.isEmpty()) {
            tagRepository.saveAll(allTags);
        }

        List<WorkNote> allWorkNotes = workNoteRepository.findAll();
        for (WorkNote workNote : allWorkNotes) {
            if (workNote.getWorkspaceId() == null) {
                workNote.setWorkspaceId(wsId);
            }
        }
        if (!allWorkNotes.isEmpty()) {
            workNoteRepository.saveAll(allWorkNotes);
        }

        log.info("Public workspace migration complete — assigned {} articles, {} tickets, {} tags, {} work notes",
            allArticles.size(), allTickets.size(), allTags.size(), allWorkNotes.size());
    }
}
