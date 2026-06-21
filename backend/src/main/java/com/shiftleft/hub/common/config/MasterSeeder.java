package com.shiftleft.hub.common.config;

import com.shiftleft.hub.ai.domain.AiConfig;
import com.shiftleft.hub.ai.domain.AiConfigRepository;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.tag.domain.TagRepository;
import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
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
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Master data seeder for the Shift-Left Knowledge Hub.
 *
 * <p>Creates seed users (7), workspaces (4), user-workspace assignments,
 * default AI config, and cleans up old seed articles.
 *
 * <p>Replaces the old DataSeeder and KbSeeder with a single synchronized seeder
 * that runs at {@code @Order(1)} during application startup.
 *
 * <p>Fully idempotent — safe to run on every startup. Checks existence by
 * unique fields before creating any entity.
 */
@Component
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class MasterSeeder {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AiConfigRepository aiConfigRepository;
    private final WorkspaceService workspaceService;
    private final WorkspaceRepository workspaceRepository;
    private final ArticleRepository articleRepository;
    private final TagRepository tagRepository;

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

    private static final String PUBLIC_SLUG = "public";

    private static final List<UserSeed> NON_ADMIN_USERS = List.of(
        new UserSeed("hr.user@company.com", "HR User", UserRole.ROLE_USER),
        new UserSeed("hr.tech@company.com", "HR Tech", UserRole.ROLE_AGENT),
        new UserSeed("legal.user@company.com", "Legal User", UserRole.ROLE_USER),
        new UserSeed("legal.tech@company.com", "Legal Tech", UserRole.ROLE_AGENT),
        new UserSeed("it.user@company.com", "IT User", UserRole.ROLE_USER),
        new UserSeed("it.tech@company.com", "IT Tech", UserRole.ROLE_AGENT)
    );

    private static final List<WorkspaceSeed> WORKSPACES = List.of(
        new WorkspaceSeed("Human Resources", "human-resources",
            "HR department — recruitment, benefits, policies, training", "groups"),
        new WorkspaceSeed("Legal", "legal",
            "Legal department — compliance, contracts, governance", "gavel"),
        new WorkspaceSeed("IT", "it",
            "IT department — infrastructure, security, support", "computer"),
        new WorkspaceSeed("Public", PUBLIC_SLUG,
            "Public workspace — general knowledge for all users", "public")
    );

    private static final List<String> OLD_ARTICLE_SLUGS = List.of(
        "connect-corporate-wifi",
        "software-installation-request",
        "reset-vpn-password",
        "company-email-mobile-setup",
        "report-security-incident",
        "printer-troubleshooting",
        "remote-access-intranet",
        "password-policy-account-security",
        "laptop-docking-station-setup"
    );

    /**
     * Main entry point for master seeding.
     *
     * <p>Runs at application startup, orchestrating user creation, workspace
     * creation, workspace-member assignments, AI config setup, and old
     * article cleanup. Skips entirely if admin email or password config
     * values are not set.
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void seed() {
        if (adminEmail == null || adminPassword == null) {
            log.info("Master seeder skipped — set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD to seed data");
            return;
        }

        try {
            log.info("Master seeder starting...");

            // Step 1: Seed 7 users
            seedUsers();

            // Step 2: Seed 4 workspaces
            seedWorkspaces();

            // Step 3: Assign users to workspaces and set default workspace
            assignUsersAndSetDefaults();

            // Step 4: Seed default AI config if none exists
            seedAiConfig();

            // Step 5: Clean up old v1.0 seed articles
            cleanupOldArticles();

            log.info("Master seeder completed successfully");
        } catch (Exception e) {
            log.error("Master seeder failed — continuing startup without seed data", e);
        }
    }

    // =========================================================================
    // Step 1: Users
    // =========================================================================

    private void seedUsers() {
        // Create admin user first (uses APP_ADMIN_EMAIL env var)
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                .email(adminEmail)
                .password(passwordEncoder.encode(adminPassword))
                .displayName("System Admin")
                .role(UserRole.ROLE_ADMIN)
                .enabled(true)
                .build();
            userRepository.save(admin);
            log.info("Created admin seed user: {}", adminEmail);
        }

        for (UserSeed us : NON_ADMIN_USERS) {
            if (!userRepository.existsByEmail(us.email())) {
                String seedPassword = generateSeedPassword();
                User user = User.builder()
                    .email(us.email())
                    .password(passwordEncoder.encode(seedPassword))
                    .displayName(us.displayName())
                    .role(us.role())
                    .enabled(true)
                    .build();
                userRepository.save(user);
                log.info("Created seed user: {} ({}) — role: {} — initial password: {}",
                    us.email(), us.displayName(), us.role(), seedPassword);
            } else {
                log.debug("Seed user {} already exists — skipping", us.email());
            }
        }
    }

    private static final java.security.SecureRandom RANDOM = new java.security.SecureRandom();
    private static final String SEED_PASSWORD_ALPHABET =
        "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghjkmnpqrstuvwxyz23456789";

    private static String generateSeedPassword() {
        StringBuilder sb = new StringBuilder(24);
        for (int i = 0; i < 24; i++) {
            sb.append(SEED_PASSWORD_ALPHABET.charAt(RANDOM.nextInt(SEED_PASSWORD_ALPHABET.length())));
        }
        return sb.toString();
    }

    // =========================================================================
    // Step 2: Workspaces
    // =========================================================================

    private void seedWorkspaces() {
        User admin = userRepository.findByEmail(adminEmail)
            .orElseThrow(() -> new IllegalStateException(
                "Admin user not found after seeding — cannot create workspaces"));

        UUID adminId = admin.getId();

        for (WorkspaceSeed ws : WORKSPACES) {
            if (workspaceService.findBySlug(ws.slug()).isEmpty()) {
                // createWorkspace generates slug from name and assigns creator as ADMIN
                Workspace workspace = workspaceService.createWorkspace(
                    ws.name(), ws.description(), null, adminId);
                // Set icon after creation (createWorkspace does not accept icon)
                workspace.setIcon(ws.icon());
                workspaceRepository.save(workspace);
                log.info("Created workspace: {} (slug: {})", ws.name(), ws.slug());
            } else {
                log.debug("Workspace {} (slug: {}) already exists — skipping", ws.name(), ws.slug());
            }
        }
    }

    // =========================================================================
    // Step 3: Assignments + default workspace
    // =========================================================================

    private void assignUsersAndSetDefaults() {
        // Build slug → Workspace map for lookups
        Map<String, Workspace> workspaceBySlug = WORKSPACES.stream()
            .map(ws -> workspaceService.findBySlug(ws.slug()).orElse(null))
            .filter(w -> w != null)
            .collect(Collectors.toMap(Workspace::getSlug, w -> w));

        Workspace publicWs = workspaceBySlug.get(PUBLIC_SLUG);
        if (publicWs == null) {
            log.warn("Public workspace not found — skipping assignments and default workspace setup");
            return;
        }

        // Admin → all workspaces as ADMIN (already done by createWorkspace but explicit for clarity)
        User admin = userRepository.findByEmail(adminEmail).orElse(null);
        if (admin != null) {
            for (Workspace ws : workspaceBySlug.values()) {
                workspaceService.assignUserToWorkspace(ws.getId(), admin.getId(), "ADMIN");
            }
        }

        // Department assignments: each department user only gets their department + Public
        assignDepartment("human-resources", workspaceBySlug, publicWs,
            "hr.user@company.com", "hr.tech@company.com");
        assignDepartment("legal", workspaceBySlug, publicWs,
            "legal.user@company.com", "legal.tech@company.com");
        assignDepartment("it", workspaceBySlug, publicWs,
            "it.user@company.com", "it.tech@company.com");

        // Set default_workspace_id to Public for all seed users (admin + non-admin)
        UUID publicWsId = publicWs.getId();
        userRepository.findByEmail(adminEmail).ifPresent(user -> {
            if (!publicWsId.equals(user.getDefaultWorkspaceId())) {
                user.setDefaultWorkspaceId(publicWsId);
                userRepository.save(user);
                log.debug("Set default workspace for {} to Public", adminEmail);
            }
        });
        for (UserSeed us : NON_ADMIN_USERS) {
            userRepository.findByEmail(us.email()).ifPresent(user -> {
                if (!publicWsId.equals(user.getDefaultWorkspaceId())) {
                    user.setDefaultWorkspaceId(publicWsId);
                    userRepository.save(user);
                    log.debug("Set default workspace for {} to Public", us.email());
                }
            });
        }
    }

    private void assignDepartment(String deptSlug, Map<String, Workspace> workspaceBySlug,
                                   Workspace publicWs, String... userEmails) {
        Workspace deptWs = workspaceBySlug.get(deptSlug);
        if (deptWs == null) {
            log.warn("Department workspace '{}' not found — skipping assignments", deptSlug);
            return;
        }
        for (String email : userEmails) {
            userRepository.findByEmail(email).ifPresent(user -> {
                workspaceService.assignUserToWorkspace(deptWs.getId(), user.getId(), "MEMBER");
                workspaceService.assignUserToWorkspace(publicWs.getId(), user.getId(), "MEMBER");
            });
        }
    }

    // =========================================================================
    // Step 4: AI Config
    // =========================================================================

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
            log.info("Created default AI config (Ollama — {} / {})",
                config.getChatModelName(), config.getEmbeddingModelName());
        } else {
            log.debug("AI config already exists — skipping");
        }
    }

    // =========================================================================
    // Step 5: Old article cleanup
    // =========================================================================

    private void cleanupOldArticles() {
        boolean anyDeleted = false;
        for (String slug : OLD_ARTICLE_SLUGS) {
            var existing = articleRepository.findBySlug(slug);
            if (existing.isPresent()) {
                articleRepository.delete(existing.get());
                anyDeleted = true;
                log.info("Deleted old seed article: {}", slug);
            }
        }
        if (anyDeleted) {
            log.info("Old seed article cleanup complete — deleted {} articles", OLD_ARTICLE_SLUGS.size());
        } else {
            log.debug("No old seed articles found — cleanup skipped");
        }
    }

    // =========================================================================
    // Internal records for seed data definitions
    // =========================================================================

    private record UserSeed(String email, String displayName, UserRole role) {
    }

    private record WorkspaceSeed(String name, String slug, String description, String icon) {
    }
}
