package com.shiftleft.hub.workspace.api;

import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.workspace.api.dto.WorkspaceInvitationResponse;
import com.shiftleft.hub.workspace.api.dto.WorkspaceResponse;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.service.WorkspaceInvitationService;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;
    private final WorkspaceInvitationService invitationService;
    private final UserRepository userRepository;

    /** Lists workspaces the current user belongs to. */
    @GetMapping("/mine")
    public ResponseEntity<List<WorkspaceResponse>> listMyWorkspaces(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        List<Workspace> workspaces = workspaceService.findWorkspacesByUserId(user.getId());
        List<WorkspaceResponse> responses = workspaces.stream()
            .map(w -> new WorkspaceResponse(
                w.getId(), w.getName(), w.getSlug(),
                w.getDescription(), w.getLogoUrl(), w.getIcon(),
                workspaceService.getMemberCount(w.getId()),
                w.getCreatedBy(), w.getCreatedAt(), w.getUpdatedAt()))
            .toList();
        return ResponseEntity.ok(responses);
    }

    /** Returns the current user's role in the active workspace. */
    @GetMapping("/current/role")
    public ResponseEntity<Map<String, String>> getCurrentRole(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        String role = workspaceService.getUserRole(workspaceId, user.getId()).orElse("NONE");
        return ResponseEntity.ok(Map.of("role", role));
    }

    /** Leaves the specified workspace. Cannot leave Default Workspace. */
    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveWorkspace(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        workspaceService.leaveWorkspace(id, user.getId());
        return ResponseEntity.ok().build();
    }
}

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final WorkspaceInvitationService invitationService;
    private final UserRepository userRepository;

    /** Lists pending invitations for the current user. */
    @GetMapping
    public ResponseEntity<List<WorkspaceInvitationResponse>> listMyInvitations(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        var invitations = invitationService.listPendingForUser(user.getId());
        return ResponseEntity.ok(invitationService.toResponseList(invitations));
    }

    /** Accepts a pending invitation. */
    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> acceptInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        invitationService.acceptInvitation(id, user.getId());
        return ResponseEntity.ok().build();
    }

    /** Rejects a pending invitation. */
    @PostMapping("/{id}/reject")
    public ResponseEntity<Void> rejectInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        invitationService.rejectInvitation(id, user.getId());
        return ResponseEntity.ok().build();
    }
}
