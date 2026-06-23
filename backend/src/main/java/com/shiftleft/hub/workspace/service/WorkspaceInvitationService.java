package com.shiftleft.hub.workspace.service;

import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.workspace.api.dto.WorkspaceInvitationResponse;
import com.shiftleft.hub.workspace.domain.InvitationNotFoundException;
import com.shiftleft.hub.workspace.domain.WorkspaceInvitation;
import com.shiftleft.hub.workspace.domain.WorkspaceInvitationRepository;
import com.shiftleft.hub.workspace.domain.WorkspaceMember;
import com.shiftleft.hub.workspace.domain.WorkspaceMemberRepository;
import com.shiftleft.hub.workspace.domain.WorkspaceNotFoundException;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Service for workspace invitation lifecycle management.
 * Handles send, list, accept, reject, and revoke operations.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class WorkspaceInvitationService {

    private final WorkspaceInvitationRepository invitationRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final UserRepository userRepository;

    /**
     * Sends a workspace invitation to a user.
     *
     * @param workspaceId   the workspace UUID
     * @param invitedUserId the invited user UUID
     * @param invitedByUserId the inviting user UUID
     * @param role          the role to assign
     * @return the created invitation entity
     */
    public WorkspaceInvitation sendInvitation(UUID workspaceId, UUID invitedUserId, UUID invitedByUserId, String role) {
        var workspace = workspaceRepository.findById(workspaceId)
            .orElseThrow(() -> new WorkspaceNotFoundException(workspaceId));
        if (workspace.getDeletedAt() != null) {
            throw new IllegalArgumentException("Cannot invite to a deleted workspace");
        }

        boolean alreadyMember = workspaceMemberRepository
            .findByIdWorkspaceIdAndIdUserId(workspaceId, invitedUserId).isPresent();
        if (alreadyMember) {
            throw new IllegalArgumentException("User is already a member of this workspace");
        }

        boolean pendingExists = invitationRepository
            .existsByWorkspaceIdAndInvitedUserIdAndStatus(workspaceId, invitedUserId, "PENDING");
        if (pendingExists) {
            throw new IllegalArgumentException("A pending invitation already exists for this user");
        }

        String normalizedRole = role != null ? role.toUpperCase() : "MEMBER";
        if (!Set.of("ADMIN", "MEMBER", "READ_ONLY").contains(normalizedRole)) {
            throw new IllegalArgumentException("Invalid role: " + role);
        }
        WorkspaceInvitation invitation = WorkspaceInvitation.builder()
            .workspaceId(workspaceId)
            .invitedUserId(invitedUserId)
            .invitedBy(invitedByUserId)
            .role(normalizedRole)
            .status("PENDING")
            .build();
        invitation = invitationRepository.save(invitation);
        log.info("Invitation sent: user {} invited to workspace {} by {} with role {}",
            invitedUserId, workspaceId, invitedByUserId, role);
        return invitation;
    }

    /**
     * Lists all invitations for a workspace.
     *
     * @param workspaceId the workspace UUID
     * @return list of invitations
     */
    public List<WorkspaceInvitation> listByWorkspace(UUID workspaceId) {
        return invitationRepository.findByWorkspaceId(workspaceId);
    }

    /**
     * Lists pending invitations for a user.
     *
     * @param userId the user UUID
     * @return list of pending invitations
     */
    public List<WorkspaceInvitation> listPendingForUser(UUID userId) {
        return invitationRepository.findByInvitedUserIdAndStatus(userId, "PENDING");
    }

    /**
     * Accepts a pending invitation, creating a workspace membership record.
     *
     * @param invitationId the invitation UUID
     * @param userId       the accepting user UUID
     * @return the updated invitation
     */
    public WorkspaceInvitation acceptInvitation(UUID invitationId, UUID userId) {
        WorkspaceInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new InvitationNotFoundException(invitationId));
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new IllegalArgumentException("Invitation is no longer pending");
        }
        if (!invitation.getInvitedUserId().equals(userId)) {
            throw new IllegalArgumentException("Cannot accept someone else's invitation");
        }

        WorkspaceMember member = WorkspaceMember.builder()
            .id(new WorkspaceMember.WorkspaceMemberId(invitation.getWorkspaceId(), userId))
            .role(invitation.getRole())
            .build();
        workspaceMemberRepository.save(member);

        invitation.setStatus("ACCEPTED");
        invitation = invitationRepository.save(invitation);
        log.info("Invitation {} accepted by user {}", invitationId, userId);
        return invitation;
    }

    /**
     * Rejects a pending invitation.
     *
     * @param invitationId the invitation UUID
     * @param userId       the rejecting user UUID
     * @return the updated invitation
     */
    public WorkspaceInvitation rejectInvitation(UUID invitationId, UUID userId) {
        WorkspaceInvitation invitation = invitationRepository.findById(invitationId)
            .orElseThrow(() -> new InvitationNotFoundException(invitationId));
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new IllegalArgumentException("Invitation is no longer pending");
        }
        if (!invitation.getInvitedUserId().equals(userId)) {
            throw new IllegalArgumentException("Cannot reject someone else's invitation");
        }

        invitation.setStatus("REJECTED");
        invitation = invitationRepository.save(invitation);
        log.info("Invitation {} rejected by user {}", invitationId, userId);
        return invitation;
    }

    /**
     * Revokes a pending invitation (admin action).
     *
     * @param invitationId the invitation UUID
     * @param workspaceId  the workspace UUID
     */
    public void revokeInvitation(UUID invitationId, UUID workspaceId) {
        WorkspaceInvitation invitation = invitationRepository.findByIdAndWorkspaceId(invitationId, workspaceId)
            .orElseThrow(() -> new InvitationNotFoundException("Invitation not found for this workspace"));
        if (!"PENDING".equals(invitation.getStatus())) {
            throw new IllegalArgumentException("Only pending invitations can be revoked");
        }
        invitation.setStatus("REVOKED");
        invitationRepository.save(invitation);
        log.info("Invitation {} revoked by admin", invitationId);
    }

    /**
     * Converts a WorkspaceInvitation entity to a response DTO with user display info.
     *
     * @param invitation the invitation entity
     * @return the response DTO
     */
    public WorkspaceInvitationResponse toResponse(WorkspaceInvitation invitation) {
        var invitedUser = userRepository.findById(invitation.getInvitedUserId()).orElse(null);
        if (invitedUser == null) {
            log.warn("Invitation {} references deleted user {}", invitation.getId(), invitation.getInvitedUserId());
        }
        return new WorkspaceInvitationResponse(
            invitation.getId(),
            invitation.getWorkspaceId(),
            invitation.getInvitedUserId(),
            invitedUser != null ? invitedUser.getEmail() : "[deleted]",
            invitedUser != null ? invitedUser.getDisplayName() : "[deleted]",
            invitation.getInvitedBy(),
            invitation.getRole(),
            invitation.getStatus(),
            invitation.getCreatedAt()
        );
    }

    /**
     * Converts a list of invitations to response DTOs.
     *
     * @param invitations the invitation entities
     * @return list of response DTOs
     */
    public List<WorkspaceInvitationResponse> toResponseList(List<WorkspaceInvitation> invitations) {
        return invitations.stream().map(this::toResponse).toList();
    }
}
