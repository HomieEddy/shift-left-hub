package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceSeeder {

    public static final String PUBLIC_SLUG = "public";

    private static final List<WorkspaceSeed> WORKSPACES = List.of(
        new WorkspaceSeed("Human Resources", "human-resources",
            "HR department - recruitment, benefits, policies, training", "groups"),
        new WorkspaceSeed("Legal", "legal",
            "Legal department - compliance, contracts, governance", "gavel"),
        new WorkspaceSeed("IT", "it",
            "IT department - infrastructure, security, support", "computer"),
        new WorkspaceSeed("Public", PUBLIC_SLUG,
            "Public workspace - general knowledge for all users", "public")
    );

    private final WorkspaceService workspaceService;
    private final WorkspaceRepository workspaceRepository;

    /**
     * Seeds all 4 workspaces. Skips any that already exist by slug.
     *
     * @param admin the user to assign as creator/ADMIN of each workspace
     */
    public void seedWorkspaces(User admin) {
        UUID adminId = admin.getId();
        for (WorkspaceSeed ws : WORKSPACES) {
            if (workspaceService.findBySlug(ws.slug()).isPresent()) {
                log.debug("Workspace {} (slug: {}) already exists - skipping", ws.name(), ws.slug());
                continue;
            }
            Workspace workspace = workspaceService.createWorkspace(
                ws.name(), ws.description(), null, adminId);
            workspace.setIcon(ws.icon());
            workspaceRepository.save(workspace);
            log.info("Created workspace: {} (slug: {})", ws.name(), ws.slug());
        }
    }

    /**
     * Returns the canonical list of workspace seeds (for callers that need to look up by slug).
     *
     * @return the immutable list of workspace seed definitions
     */
    public static List<WorkspaceSeed> workspaceSeeds() {
        return WORKSPACES;
    }

    /**
     * Immutable description of a workspace to be created during seeding.
     *
     * @param name        the human-readable workspace name
     * @param slug        the URL-safe slug
     * @param description the long-form description
     * @param icon        the Material icon name to render in the UI
     */
    public record WorkspaceSeed(String name, String slug, String description, String icon) {
    }
}