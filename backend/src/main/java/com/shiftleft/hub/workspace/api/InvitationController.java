package com.shiftleft.hub.workspace.api;

import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.workspace.api.dto.WorkspaceInvitationResponse;
import com.shiftleft.hub.workspace.service.WorkspaceInvitationService;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/invitations")
@RequiredArgsConstructor
public class InvitationController {

    private final WorkspaceInvitationService invitationService;
    private final UserRepository userRepository;

    /**
     * Lists pending invitations for the current user.
     *
     * @param userDetails the authenticated user
     * @return list of pending invitation responses
     */
    @GetMapping
    public ResponseEntity<List<WorkspaceInvitationResponse>> listMyInvitations(
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        var invitations = invitationService.listPendingForUser(user.getId());
        return ResponseEntity.ok(invitationService.toResponseList(invitations));
    }

    /**
     * Accepts a pending invitation, creating a workspace membership.
     *
     * @param id the invitation UUID
     * @param userDetails the authenticated user
     * @return HTTP 200 on success
     */
    @PostMapping("/{id}/accept")
    public ResponseEntity<Void> acceptInvitation(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserDetails userDetails) {
        var user = userRepository.findByEmail(userDetails.getUsername())
            .orElseThrow(() -> new RuntimeException("User not found"));
        invitationService.acceptInvitation(id, user.getId());
        return ResponseEntity.ok().build();
    }

    /**
     * Rejects a pending invitation.
     *
     * @param id the invitation UUID
     * @param userDetails the authenticated user
     * @return HTTP 200 on success
     */
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
