package com.shiftleft.hub.workspace.service;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.user.domain.UserRole;
import com.shiftleft.hub.workspace.api.dto.WorkspaceInvitationResponse;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceInvitation;
import com.shiftleft.hub.workspace.domain.WorkspaceInvitationRepository;
import com.shiftleft.hub.workspace.domain.WorkspaceMember;
import com.shiftleft.hub.workspace.domain.WorkspaceMemberRepository;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceInvitationServiceTest {

    @Mock private WorkspaceInvitationRepository invitationRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;
    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private WorkspaceInvitationService workspaceInvitationService;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID invitedUserId = UUID.randomUUID();
    private final UUID invitedByUserId = UUID.randomUUID();
    private final UUID invitationId = UUID.randomUUID();

    private Workspace createActiveWorkspace() {
        return Workspace.builder()
            .id(workspaceId)
            .name("Test Workspace")
            .slug("test-workspace")
            .createdBy(invitedByUserId)
            .build();
    }

    private WorkspaceInvitation createPendingInvitation() {
        return WorkspaceInvitation.builder()
            .id(invitationId)
            .workspaceId(workspaceId)
            .invitedUserId(invitedUserId)
            .invitedBy(invitedByUserId)
            .role("MEMBER")
            .status("PENDING")
            .createdAt(LocalDateTime.now())
            .build();
    }

    // ── sendInvitation ────────────────────────────────────────

    @Test
    void sendInvitation_shouldSucceed() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(createActiveWorkspace()));
        when(workspaceMemberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, invitedUserId))
            .thenReturn(Optional.empty());
        when(invitationRepository.existsByWorkspaceIdAndInvitedUserIdAndStatus(workspaceId, invitedUserId, "PENDING"))
            .thenReturn(false);
        when(invitationRepository.save(any(WorkspaceInvitation.class)))
            .thenReturn(createPendingInvitation());

        WorkspaceInvitation result = workspaceInvitationService.sendInvitation(
            workspaceId, invitedUserId, invitedByUserId, "MEMBER");

        assertNotNull(result);
        assertEquals("PENDING", result.getStatus());
        assertEquals(workspaceId, result.getWorkspaceId());
        assertEquals(invitedUserId, result.getInvitedUserId());
        verify(invitationRepository).save(any(WorkspaceInvitation.class));
    }

    @Test
    void sendInvitation_shouldThrowWhenWorkspaceDeleted() {
        Workspace deleted = createActiveWorkspace();
        deleted.setDeletedAt(LocalDateTime.now());
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(deleted));

        assertThrows(IllegalArgumentException.class,
            () -> workspaceInvitationService.sendInvitation(workspaceId, invitedUserId, invitedByUserId, "MEMBER"));
        verify(invitationRepository, never()).save(any());
    }

    // ── listPendingForUser ────────────────────────────────────

    @Test
    void getPendingInvitations_shouldReturnList() {
        WorkspaceInvitation inv = createPendingInvitation();
        when(invitationRepository.findByInvitedUserIdAndStatus(invitedUserId, "PENDING"))
            .thenReturn(List.of(inv));

        List<WorkspaceInvitation> result = workspaceInvitationService.listPendingForUser(invitedUserId);

        assertEquals(1, result.size());
        assertEquals(invitationId, result.getFirst().getId());
    }

    @Test
    void getPendingInvitations_shouldReturnEmptyWhenNone() {
        when(invitationRepository.findByInvitedUserIdAndStatus(invitedUserId, "PENDING"))
            .thenReturn(List.of());

        List<WorkspaceInvitation> result = workspaceInvitationService.listPendingForUser(invitedUserId);

        assertTrue(result.isEmpty());
    }

    // ── acceptInvitation ──────────────────────────────────────

    @Test
    void acceptInvitation_shouldSucceed() {
        WorkspaceInvitation invitation = createPendingInvitation();
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class))).thenReturn(null);
        when(invitationRepository.save(any(WorkspaceInvitation.class))).thenReturn(invitation);

        WorkspaceInvitation result = workspaceInvitationService.acceptInvitation(invitationId, invitedUserId);

        assertEquals("ACCEPTED", result.getStatus());
        ArgumentCaptor<WorkspaceMember> memberCaptor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(memberCaptor.capture());
        assertEquals(workspaceId, memberCaptor.getValue().getId().getWorkspaceId());
        assertEquals(invitedUserId, memberCaptor.getValue().getId().getUserId());
        assertEquals("MEMBER", memberCaptor.getValue().getRole());
    }

    @Test
    void acceptInvitation_shouldThrowWhenAlreadyAccepted() {
        WorkspaceInvitation invitation = createPendingInvitation();
        invitation.setStatus("ACCEPTED");
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThrows(IllegalArgumentException.class,
            () -> workspaceInvitationService.acceptInvitation(invitationId, invitedUserId));
        verify(workspaceMemberRepository, never()).save(any());
    }

    @Test
    void acceptInvitation_shouldThrowWhenTokenInvalid() {
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class,
            () -> workspaceInvitationService.acceptInvitation(invitationId, invitedUserId));
        verify(workspaceMemberRepository, never()).save(any());
    }

    @Test
    void acceptInvitation_shouldThrowWhenWrongUser() {
        WorkspaceInvitation invitation = createPendingInvitation();
        UUID wrongUserId = UUID.randomUUID();
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThrows(IllegalArgumentException.class,
            () -> workspaceInvitationService.acceptInvitation(invitationId, wrongUserId));
        verify(workspaceMemberRepository, never()).save(any());
    }

    // ── rejectInvitation ──────────────────────────────────────

    @Test
    void rejectInvitation_shouldSucceed() {
        WorkspaceInvitation invitation = createPendingInvitation();
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(WorkspaceInvitation.class))).thenReturn(invitation);

        WorkspaceInvitation result = workspaceInvitationService.rejectInvitation(invitationId, invitedUserId);

        assertEquals("REJECTED", result.getStatus());
        verify(invitationRepository).save(invitation);
    }

    @Test
    void rejectInvitation_shouldThrowWhenNotPending() {
        WorkspaceInvitation invitation = createPendingInvitation();
        invitation.setStatus("ACCEPTED");
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThrows(IllegalArgumentException.class,
            () -> workspaceInvitationService.rejectInvitation(invitationId, invitedUserId));
        verify(invitationRepository, never()).save(any());
    }

    @Test
    void rejectInvitation_shouldThrowWhenWrongUser() {
        WorkspaceInvitation invitation = createPendingInvitation();
        UUID wrongUserId = UUID.randomUUID();
        when(invitationRepository.findById(invitationId)).thenReturn(Optional.of(invitation));

        assertThrows(IllegalArgumentException.class,
            () -> workspaceInvitationService.rejectInvitation(invitationId, wrongUserId));
        verify(invitationRepository, never()).save(any());
    }

    // ── revokeInvitation ──────────────────────────────────────

    @Test
    void revokeInvitation_shouldSucceed() {
        WorkspaceInvitation invitation = createPendingInvitation();
        when(invitationRepository.findByIdAndWorkspaceId(invitationId, workspaceId))
            .thenReturn(Optional.of(invitation));
        when(invitationRepository.save(any(WorkspaceInvitation.class))).thenReturn(invitation);

        workspaceInvitationService.revokeInvitation(invitationId, workspaceId);

        assertEquals("REVOKED", invitation.getStatus());
        verify(invitationRepository).save(invitation);
    }

    // ── toResponse ────────────────────────────────────────────

    @Test
    void toResponse_shouldIncludeUserDisplayInfo() {
        WorkspaceInvitation invitation = createPendingInvitation();
        User user = User.builder()
            .id(invitedUserId).email("user@example.com")
            .password("pwd").displayName("Display Name")
            .role(UserRole.ROLE_USER).enabled(true).build();
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.of(user));

        WorkspaceInvitationResponse response = workspaceInvitationService.toResponse(invitation);

        assertEquals(invitationId, response.id());
        assertEquals(workspaceId, response.workspaceId());
        assertEquals("user@example.com", response.invitedUserEmail());
        assertEquals("Display Name", response.invitedUserDisplayName());
    }

    @Test
    void toResponse_shouldHandleDeletedUser() {
        WorkspaceInvitation invitation = createPendingInvitation();
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.empty());

        WorkspaceInvitationResponse response = workspaceInvitationService.toResponse(invitation);

        assertEquals("[deleted]", response.invitedUserEmail());
        assertEquals("[deleted]", response.invitedUserDisplayName());
    }

    // ── toResponseList ────────────────────────────────────────

    @Test
    void toResponseList_shouldConvertMultiple() {
        WorkspaceInvitation invitation = createPendingInvitation();
        when(userRepository.findById(invitedUserId)).thenReturn(Optional.empty());

        List<WorkspaceInvitationResponse> responses =
            workspaceInvitationService.toResponseList(List.of(invitation));

        assertEquals(1, responses.size());
        assertEquals(invitationId, responses.getFirst().id());
    }

    @Test
    void toResponseList_shouldReturnEmptyWhenGivenEmpty() {
        List<WorkspaceInvitationResponse> responses =
            workspaceInvitationService.toResponseList(List.of());

        assertTrue(responses.isEmpty());
    }
}
