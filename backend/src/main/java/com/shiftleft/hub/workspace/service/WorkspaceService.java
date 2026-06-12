package com.shiftleft.hub.workspace.service;

import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceMember;
import com.shiftleft.hub.workspace.domain.WorkspaceMemberRepository;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Service for workspace management operations. */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WorkspaceService {

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    /**
     * Creates a new workspace with a URL-safe slug generated from the name.
     * The creator is automatically added as an ADMIN member.
     *
     * @param name the workspace name
     * @param description optional description
     * @param logoUrl optional logo URL
     * @param createdBy the UUID of the creating user
     * @return the created workspace entity
     */
    public Workspace createWorkspace(String name, String description, String logoUrl, UUID createdBy) {
        String slug = generateUniqueSlug(name);
        Workspace workspace = Workspace.builder()
            .name(name)
            .slug(slug)
            .description(description)
            .logoUrl(logoUrl)
            .createdBy(createdBy)
            .build();
        workspace = workspaceRepository.save(workspace);

        WorkspaceMember member = WorkspaceMember.builder()
            .id(new WorkspaceMember.WorkspaceMemberId(workspace.getId(), createdBy))
            .role("ADMIN")
            .build();
        workspaceMemberRepository.save(member);

        log.info("Created workspace '{}' (slug: {}) with admin user {}", name, slug, createdBy);
        return workspace;
    }

    public Workspace save(Workspace workspace) {
        return workspaceRepository.save(workspace);
    }

    public List<Workspace> findAll() {
        return workspaceRepository.findAll();
    }

    public Optional<Workspace> findById(UUID id) {
        return workspaceRepository.findById(id);
    }

    public Optional<Workspace> findBySlug(String slug) {
        return workspaceRepository.findBySlug(slug);
    }

    public boolean existsBySlug(String slug) {
        return workspaceRepository.existsBySlug(slug);
    }

    public boolean isMemberOfWorkspace(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, userId).isPresent();
    }

    public List<WorkspaceMember> getWorkspaceMembers(UUID workspaceId) {
        return workspaceMemberRepository.findByIdWorkspaceId(workspaceId);
    }

    public long getMemberCount(UUID workspaceId) {
        return workspaceMemberRepository.countByIdWorkspaceId(workspaceId);
    }

    /**
     * Assigns a user to the given workspace with the specified role.
     * If the user is already a member, updates their role.
     *
     * @param workspaceId the workspace UUID
     * @param userId the user UUID
     * @param role the role to assign (ADMIN, MEMBER, or READ_ONLY)
     */
    public void assignUserToWorkspace(UUID workspaceId, UUID userId, String role) {
        WorkspaceMember member = workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
            .orElse(WorkspaceMember.builder()
                .id(new WorkspaceMember.WorkspaceMemberId(workspaceId, userId))
                .build());
        member.setRole(role.toUpperCase());
        workspaceMemberRepository.save(member);
        log.info("Assigned user {} to workspace {} with role {}", userId, workspaceId, role);
    }

    /**
     * Updates workspace name, description, and/or icon.
     * Only non-null fields are updated. The Default Workspace cannot be renamed.
     *
     * @param id workspace UUID
     * @param name new name, or null to keep existing
     * @param description new description, or null to keep existing
     * @param icon new icon name, or null to keep existing
     * @return the updated workspace entity
     */
    public Workspace updateWorkspace(UUID id, String name, String description, String icon) {
        Workspace workspace = workspaceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workspace not found: " + id));
        if ("default".equals(workspace.getSlug()) && name != null) {
            throw new IllegalArgumentException("Cannot rename the Default Workspace");
        }
        if (name != null) {
            workspace.setName(name);
        }
        if (description != null) {
            workspace.setDescription(description);
        }
        if (icon != null) {
            workspace.setIcon(icon);
        }
        return workspaceRepository.save(workspace);
    }

    /**
     * Soft-deletes a workspace by setting its deleted_at timestamp.
     * The Default Workspace cannot be deleted.
     *
     * @param id workspace UUID
     * @return the soft-deleted workspace entity
     */
    public Workspace softDeleteWorkspace(UUID id) {
        Workspace workspace = workspaceRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Workspace not found: " + id));
        if ("default".equals(workspace.getSlug())) {
            throw new IllegalArgumentException("Cannot delete the Default Workspace");
        }
        workspace.setDeletedAt(LocalDateTime.now());
        return workspaceRepository.save(workspace);
    }

    /**
     * Removes the current user from a workspace. Enforces only-admin guard.
     * The Default Workspace cannot be left.
     *
     * @param workspaceId workspace UUID
     * @param userId user UUID
     */
    public void leaveWorkspace(UUID workspaceId, UUID userId) {
        Workspace workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new RuntimeException("Workspace not found: " + workspaceId));
        if ("default".equals(workspace.getSlug())) {
            throw new IllegalArgumentException("Cannot leave the Default Workspace");
        }
        WorkspaceMember member = workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
            .orElseThrow(() -> new RuntimeException("User is not a member of this workspace"));

        long adminCount = workspaceMemberRepository.countByIdWorkspaceIdAndRole(workspaceId, "ADMIN");
        if (adminCount <= 1 && "ADMIN".equals(member.getRole())) {
            throw new IllegalArgumentException("Cannot leave — must leave at least one admin");
        }

        workspaceMemberRepository.delete(member);
        log.info("User {} left workspace {}", userId, workspaceId);
    }

    /**
     * Removes a member from a workspace. Enforces only-admin guard.
     *
     * @param workspaceId workspace UUID
     * @param userId user UUID
     */
    public void removeMember(UUID workspaceId, UUID userId) {
        WorkspaceMember member = workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
            .orElseThrow(() -> new RuntimeException("Member not found"));

        long adminCount = workspaceMemberRepository.countByIdWorkspaceIdAndRole(workspaceId, "ADMIN");
        if (adminCount <= 1 && "ADMIN".equals(member.getRole())) {
            throw new IllegalArgumentException("Cannot remove the only admin");
        }

        workspaceMemberRepository.delete(member);
        log.info("Removed user {} from workspace {}", userId, workspaceId);
    }

    /**
     * Changes a member's role in a workspace. Enforces only-admin guard on demotion.
     *
     * @param workspaceId workspace UUID
     * @param userId member user UUID
     * @param newRole new role string (ADMIN, MEMBER, READ_ONLY)
     * @return the updated WorkspaceMember entity
     */
    public WorkspaceMember changeMemberRole(UUID workspaceId, UUID userId, String newRole) {
        WorkspaceMember member = workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
            .orElseThrow(() -> new RuntimeException("Member not found"));

        long adminCount = workspaceMemberRepository.countByIdWorkspaceIdAndRole(workspaceId, "ADMIN");
        if (adminCount <= 1 && "ADMIN".equals(member.getRole()) && !"ADMIN".equals(newRole)) {
            throw new IllegalArgumentException("Cannot remove the only admin role");
        }

        member.setRole(newRole.toUpperCase());
        return workspaceMemberRepository.save(member);
    }

    /**
     * Finds all non-deleted workspaces that a user belongs to.
     *
     * @param userId user UUID
     * @return list of workspaces the user is a member of
     */
    public List<Workspace> findWorkspacesByUserId(UUID userId) {
        List<WorkspaceMember> memberships = workspaceMemberRepository.findByIdUserId(userId);
        List<UUID> workspaceIds = memberships.stream()
            .map(m -> m.getId().getWorkspaceId())
            .toList();
        return workspaceRepository.findAllByIdIn(workspaceIds).stream()
            .filter(w -> w.getDeletedAt() == null)
            .toList();
    }

    /**
     * Returns the user's role in a workspace, or empty if not a member.
     *
     * @param workspaceId workspace UUID
     * @param userId user UUID
     * @return optional role string
     */
    public Optional<String> getUserRole(UUID workspaceId, UUID userId) {
        return workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserId(workspaceId, userId)
            .map(WorkspaceMember::getRole);
    }

    /**
     * Returns all workspaces that have not been soft-deleted.
     *
     * @return list of active workspaces
     */
    public List<Workspace> findAllNonDeleted() {
        return workspaceRepository.findAllByDeletedAtIsNull();
    }

    private String generateUniqueSlug(String name) {
        String base = name.toLowerCase()
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .trim()
            .replaceAll("^-|-$", "");
        if (base.length() > 120) {
            base = base.substring(0, 120);
        }

        String slug = base;
        int suffix = 2;
        while (workspaceRepository.existsBySlug(slug)) {
            slug = base + "-" + suffix;
            suffix++;
        }
        return slug;
    }
}
