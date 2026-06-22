package com.shiftleft.hub.common.config;

import com.shiftleft.hub.common.config.seeder.AiConfigSeeder;
import com.shiftleft.hub.common.config.seeder.OldArticleCleanupSeeder;
import com.shiftleft.hub.common.config.seeder.UserSeeder;
import com.shiftleft.hub.common.config.seeder.WorkspaceAssignmentSeeder;
import com.shiftleft.hub.common.config.seeder.WorkspaceSeeder;
import com.shiftleft.hub.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Profile;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Master data seeder for the Shift-Left Knowledge Hub.
 *
 * <p>Orchestrates the 5 single-responsibility seeders in dependency order:
 * users -> workspaces -> assignments -> AI config -> old-article cleanup.
 *
 * <p>Runs at application startup, is fully idempotent (each seeder checks
 * for existing data before creating), and skips entirely if admin email
 * or password config values are not set.
 */
@Component
@RequiredArgsConstructor
@Profile("!test")
@Slf4j
public class MasterSeeder {

    private final UserSeeder userSeeder;
    private final WorkspaceSeeder workspaceSeeder;
    private final WorkspaceAssignmentSeeder workspaceAssignmentSeeder;
    private final AiConfigSeeder aiConfigSeeder;
    private final OldArticleCleanupSeeder oldArticleCleanupSeeder;

    @Value("${app.admin.email:#{null}}")
    private String adminEmail;

    @Value("${app.admin.password:#{null}}")
    private String adminPassword;

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
            log.info("Master seeder skipped - set APP_ADMIN_EMAIL and APP_ADMIN_PASSWORD to seed data");
            return;
        }

        try {
            log.info("Master seeder starting...");

            User admin = userSeeder.seedUsers(adminEmail, adminPassword);
            workspaceSeeder.seedWorkspaces(admin);
            workspaceAssignmentSeeder.assignUsersAndSetDefaults(adminEmail);
            aiConfigSeeder.seedAiConfig();
            oldArticleCleanupSeeder.cleanupOldArticles();

            log.info("Master seeder completed successfully");
        } catch (Exception e) {
            log.error("Master seeder failed - continuing startup without seed data", e);
        }
    }
}