package com.shiftleft.hub.workspace.api;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.workspace.api.dto.AssignUserRequest;
import com.shiftleft.hub.workspace.api.dto.CreateWorkspaceRequest;
import com.shiftleft.hub.workspace.api.dto.WorkspaceResponse;
import com.shiftleft.hub.workspace.api.dto.WorkspaceUserResponse;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceMember;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/** REST controller for admin workspace management endpoints. */
@RestController
@RequestMapping("/api/admin/workspaces")
@RequiredArgsConstructor
public class AdminWorkspaceController {

    private final WorkspaceService workspaceService;
    private final UserRepository userRepository;

    /**
     * Creates a new workspace.
     *
     * @param request the workspace creation payload
     * @param userDetails the authenticated admin user
     * @return the created workspace with HTTP 201
     */
    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        Workspace workspace = workspaceService.createWorkspace(
            request.name(), request.description(), request.logoUrl(), admin.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(toResponse(workspace));
    }

    /**
     * Lists all workspaces.
     *
     * @return list of workspace responses
     */
    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> listWorkspaces() {
        List<Workspace> workspaces = workspaceService.findAll();
        List<WorkspaceResponse> responses = workspaces.stream()
            .map(this::toResponse)
            .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Gets a single workspace by ID.
     *
     * @param id the workspace UUID
     * @return the workspace response
     */
    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspace(@PathVariable UUID id) {
        Workspace workspace = workspaceService.findById(id)
            .orElseThrow(() -> new RuntimeException("Workspace not found: " + id));
        return ResponseEntity.ok(toResponse(workspace));
    }

    /**
     * Lists all members of a workspace.
     *
     * @param id the workspace UUID
     * @return list of workspace member responses
     */
    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceUserResponse>> listMembers(@PathVariable UUID id) {
        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(id);
        List<WorkspaceUserResponse> responses = members.stream()
            .map(member -> {
                User user = userRepository.findById(member.getId().getUserId())
                    .orElse(null);
                return new WorkspaceUserResponse(
                    member.getId().getUserId(),
                    user != null ? user.getEmail() : "unknown",
                    user != null ? user.getDisplayName() : "Unknown",
                    member.getRole(),
                    member.getJoinedAt()
                );
            })
            .toList();
        return ResponseEntity.ok(responses);
    }

    /**
     * Assigns a user to a workspace with a specific role.
     *
     * @param id the workspace UUID
     * @param request the assignment payload with userId and role
     * @return HTTP 200 on success
     */
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> assignUser(
            @PathVariable UUID id,
            @Valid @RequestBody AssignUserRequest request) {
        workspaceService.assignUserToWorkspace(id, request.userId(), request.role());
        return ResponseEntity.ok().build();
    }

    /**
     * Lists users not yet assigned to this workspace.
     *
     * @param id the workspace UUID
     * @return list of available user responses
     */
    @GetMapping("/{id}/available-users")
    public ResponseEntity<List<WorkspaceUserResponse>> listAvailableUsers(@PathVariable UUID id) {
        List<User> users = userRepository.findUsersNotInWorkspace(id);
        List<WorkspaceUserResponse> responses = users.stream()
            .map(u -> new WorkspaceUserResponse(
                u.getId(), u.getEmail(), u.getDisplayName(), null, null))
            .toList();
        return ResponseEntity.ok(responses);
    }

    private WorkspaceResponse toResponse(Workspace w) {
        return new WorkspaceResponse(
            w.getId(), w.getName(), w.getSlug(),
            w.getDescription(), w.getLogoUrl(), w.getIcon(),
            workspaceService.getMemberCount(w.getId()),
            w.getCreatedBy(), w.getCreatedAt(), w.getUpdatedAt());
    }
}
