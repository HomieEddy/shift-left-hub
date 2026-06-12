package com.shiftleft.hub.workspace.api;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.workspace.api.dto.AssignUserRequest;
import com.shiftleft.hub.workspace.api.dto.ChangeRoleRequest;
import com.shiftleft.hub.workspace.api.dto.CreateWorkspaceRequest;
import com.shiftleft.hub.workspace.api.dto.InvitationRequest;
import com.shiftleft.hub.workspace.api.dto.UpdateWorkspaceRequest;
import com.shiftleft.hub.workspace.api.dto.WorkspaceInvitationResponse;
import com.shiftleft.hub.workspace.api.dto.WorkspaceResponse;
import com.shiftleft.hub.workspace.api.dto.WorkspaceUserResponse;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceMember;
import com.shiftleft.hub.workspace.service.WorkspaceInvitationService;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/workspaces")
@RequiredArgsConstructor
public class AdminWorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceInvitationService invitationService;
    private final UserRepository userRepository;

    /** Creates a new workspace. */
    @PostMapping
    public ResponseEntity<WorkspaceResponse> createWorkspace(
            @Valid @RequestBody CreateWorkspaceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        Workspace workspace = workspaceService.createWorkspace(
            request.name(), request.description(), request.logoUrl(), admin.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(workspace));
    }

    /** Lists all non-deleted workspaces. */
    @GetMapping
    public ResponseEntity<List<WorkspaceResponse>> listWorkspaces() {
        List<Workspace> workspaces = workspaceService.findAllNonDeleted();
        List<WorkspaceResponse> responses = workspaces.stream().map(this::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    /** Gets a single workspace by ID. */
    @GetMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> getWorkspace(@PathVariable UUID id) {
        Workspace workspace = workspaceService.findById(id)
            .orElseThrow(() -> new RuntimeException("Workspace not found: " + id));
        return ResponseEntity.ok(toResponse(workspace));
    }

    /** Updates workspace name, description, and/or icon. */
    @PutMapping("/{id}")
    public ResponseEntity<WorkspaceResponse> updateWorkspace(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateWorkspaceRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Workspace workspace = workspaceService.updateWorkspace(
            id, request.name(), request.description(), request.icon());
        return ResponseEntity.ok(toResponse(workspace));
    }

    /** Soft-deletes a workspace. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteWorkspace(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        workspaceService.softDeleteWorkspace(id);
        return ResponseEntity.noContent().build();
    }

    /** Lists all members of a workspace. */
    @GetMapping("/{id}/members")
    public ResponseEntity<List<WorkspaceUserResponse>> listMembers(@PathVariable UUID id) {
        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(id);
        List<WorkspaceUserResponse> responses = members.stream()
            .map(member -> {
                User user = userRepository.findById(member.getId().getUserId()).orElse(null);
                return new WorkspaceUserResponse(
                    member.getId().getUserId(),
                    user != null ? user.getEmail() : "unknown",
                    user != null ? user.getDisplayName() : "Unknown",
                    member.getRole(),
                    member.getJoinedAt());
            }).toList();
        return ResponseEntity.ok(responses);
    }

    /** Assigns a user to a workspace with a specific role. */
    @PostMapping("/{id}/members")
    public ResponseEntity<Void> assignUser(
            @PathVariable UUID id,
            @Valid @RequestBody AssignUserRequest request) {
        workspaceService.assignUserToWorkspace(id, request.userId(), request.role());
        return ResponseEntity.ok().build();
    }

    /** Removes a member from a workspace. */
    @DeleteMapping("/{id}/members/{userId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID id, @PathVariable UUID userId) {
        workspaceService.removeMember(id, userId);
        return ResponseEntity.noContent().build();
    }

    /** Changes a member's role in a workspace. */
    @PutMapping("/{id}/members/{userId}/role")
    public ResponseEntity<Void> changeMemberRole(
            @PathVariable UUID id,
            @PathVariable UUID userId,
            @Valid @RequestBody ChangeRoleRequest request) {
        workspaceService.changeMemberRole(id, userId, request.role());
        return ResponseEntity.ok().build();
    }

    /** Sends a workspace invitation to a user. */
    @PostMapping("/{id}/invitations")
    public ResponseEntity<WorkspaceInvitationResponse> sendInvitation(
            @PathVariable UUID id,
            @Valid @RequestBody InvitationRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        User admin = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("Admin not found"));
        var invitation = invitationService.sendInvitation(id, request.userId(), admin.getId(), request.role());
        return ResponseEntity.status(HttpStatus.CREATED).body(invitationService.toResponse(invitation));
    }

    /** Lists all invitations for a workspace. */
    @GetMapping("/{id}/invitations")
    public ResponseEntity<List<WorkspaceInvitationResponse>> listInvitations(@PathVariable UUID id) {
        var invitations = invitationService.listByWorkspace(id);
        return ResponseEntity.ok(invitationService.toResponseList(invitations));
    }

    /** Revokes a pending workspace invitation. */
    @DeleteMapping("/{id}/invitations/{invitationId}")
    public ResponseEntity<Void> revokeInvitation(@PathVariable UUID id, @PathVariable UUID invitationId) {
        invitationService.revokeInvitation(invitationId, id);
        return ResponseEntity.noContent().build();
    }

    /** Lists users not yet assigned to this workspace. */
    @GetMapping("/{id}/available-users")
    public ResponseEntity<List<WorkspaceUserResponse>> listAvailableUsers(@PathVariable UUID id) {
        List<User> users = userRepository.findUsersNotInWorkspace(id);
        List<WorkspaceUserResponse> responses = users.stream()
            .map(u -> new WorkspaceUserResponse(u.getId(), u.getEmail(), u.getDisplayName(), null, null))
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
