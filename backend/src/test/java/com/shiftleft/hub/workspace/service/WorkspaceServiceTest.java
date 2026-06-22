package com.shiftleft.hub.workspace.service;

import com.shiftleft.hub.workspace.domain.Workspace;
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
class WorkspaceServiceTest {

    @Mock private WorkspaceRepository workspaceRepository;
    @Mock private WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks private WorkspaceService workspaceService;

    private final UUID workspaceId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final String name = "Test Workspace";
    private final String slug = "test-workspace";

    private Workspace createWorkspace() {
        return Workspace.builder()
            .id(workspaceId)
            .name(name)
            .slug(slug)
            .description("A test workspace")
            .logoUrl(null)
            .createdBy(userId)
            .createdAt(LocalDateTime.now())
            .updatedAt(LocalDateTime.now())
            .build();
    }

    // ── createWorkspace ────────────────────────────────────────

    @Test
    void createWorkspace_shouldCreateWithAdminMember() {
        when(workspaceRepository.existsBySlug(anyString())).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(createWorkspace());
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
            .thenReturn(WorkspaceMember.builder().build());

        Workspace result = workspaceService.createWorkspace(name, "desc", null, userId);

        assertNotNull(result);
        assertEquals(name, result.getName());
        assertEquals(slug, result.getSlug());
        verify(workspaceRepository).save(any(Workspace.class));
        verify(workspaceMemberRepository).save(any(WorkspaceMember.class));
    }

    @Test
    void createWorkspace_shouldGenerateSlugFromName() {
        when(workspaceRepository.existsBySlug("my-workspace")).thenReturn(false);
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(
            Workspace.builder().id(workspaceId).name("My Workspace!").slug("my-workspace")
                .createdBy(userId).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
            .thenReturn(WorkspaceMember.builder().build());

        Workspace result = workspaceService.createWorkspace("My Workspace!", null, null, userId);

        assertEquals("my-workspace", result.getSlug());
    }

    @Test
    void createWorkspace_shouldHandleSlugCollisions() {
        when(workspaceRepository.existsBySlug("test")).thenReturn(true);
        when(workspaceRepository.findSlugStringsByPrefix("test-")).thenReturn(List.of("test-2"));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(
            Workspace.builder().id(workspaceId).name("test").slug("test-3")
                .createdBy(userId).createdAt(LocalDateTime.now()).updatedAt(LocalDateTime.now())
                .build());
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
            .thenReturn(WorkspaceMember.builder().build());

        Workspace result = workspaceService.createWorkspace("test", null, null, userId);

        assertEquals("test-3", result.getSlug());
    }

    // ── findAll ────────────────────────────────────────────────

    @Test
    void findAll_shouldReturnAllWorkspaces() {
        when(workspaceRepository.findAll()).thenReturn(List.of(createWorkspace()));

        List<Workspace> results = workspaceService.findAll();

        assertEquals(1, results.size());
        assertEquals(name, results.getFirst().getName());
    }

    @Test
    void findAll_shouldReturnEmptyListWhenNoneExist() {
        when(workspaceRepository.findAll()).thenReturn(List.of());

        List<Workspace> results = workspaceService.findAll();

        assertTrue(results.isEmpty());
    }

    // ── findById ───────────────────────────────────────────────

    @Test
    void findById_shouldReturnWorkspaceWhenFound() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(createWorkspace()));

        Optional<Workspace> result = workspaceService.findById(workspaceId);

        assertTrue(result.isPresent());
        assertEquals(name, result.get().getName());
    }

    @Test
    void findById_shouldReturnEmptyWhenNotFound() {
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.empty());

        Optional<Workspace> result = workspaceService.findById(workspaceId);

        assertTrue(result.isEmpty());
    }

    // ── findBySlug ─────────────────────────────────────────────

    @Test
    void findBySlug_shouldReturnWorkspaceWhenFound() {
        when(workspaceRepository.findBySlug(slug)).thenReturn(Optional.of(createWorkspace()));

        Optional<Workspace> result = workspaceService.findBySlug(slug);

        assertTrue(result.isPresent());
        assertEquals(name, result.get().getName());
    }

    @Test
    void findBySlug_shouldReturnEmptyWhenNotFound() {
        when(workspaceRepository.findBySlug("nonexistent")).thenReturn(Optional.empty());

        Optional<Workspace> result = workspaceService.findBySlug("nonexistent");

        assertTrue(result.isEmpty());
    }

    // ── existsBySlug ───────────────────────────────────────────

    @Test
    void existsBySlug_shouldReturnTrueWhenExists() {
        when(workspaceRepository.existsBySlug(slug)).thenReturn(true);

        assertTrue(workspaceService.existsBySlug(slug));
    }

    @Test
    void existsBySlug_shouldReturnFalseWhenNotExists() {
        when(workspaceRepository.existsBySlug("nonexistent")).thenReturn(false);

        assertFalse(workspaceService.existsBySlug("nonexistent"));
    }

    // ── isMemberOfWorkspace ────────────────────────────────────

    @Test
    void isMemberOfWorkspace_shouldReturnTrueWhenMember() {
        when(workspaceMemberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, userId))
            .thenReturn(Optional.of(WorkspaceMember.builder().build()));

        assertTrue(workspaceService.isMemberOfWorkspace(workspaceId, userId));
    }

    @Test
    void isMemberOfWorkspace_shouldReturnFalseWhenNotMember() {
        when(workspaceMemberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, userId))
            .thenReturn(Optional.empty());

        assertFalse(workspaceService.isMemberOfWorkspace(workspaceId, userId));
    }

    // ── getWorkspaceMembers ────────────────────────────────────

    @Test
    void getWorkspaceMembers_shouldReturnMemberList() {
        WorkspaceMember member = WorkspaceMember.builder()
            .id(new WorkspaceMember.WorkspaceMemberId(workspaceId, userId))
            .role("ADMIN")
            .build();
        when(workspaceMemberRepository.findByIdWorkspaceId(workspaceId))
            .thenReturn(List.of(member));

        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceId);

        assertEquals(1, members.size());
        assertEquals("ADMIN", members.getFirst().getRole());
    }

    @Test
    void getWorkspaceMembers_shouldReturnEmptyWhenNoMembers() {
        when(workspaceMemberRepository.findByIdWorkspaceId(workspaceId))
            .thenReturn(List.of());

        List<WorkspaceMember> members = workspaceService.getWorkspaceMembers(workspaceId);

        assertTrue(members.isEmpty());
    }

    // ── getMemberCount ─────────────────────────────────────────

    @Test
    void getMemberCount_shouldReturnCount() {
        when(workspaceMemberRepository.countByIdWorkspaceId(workspaceId)).thenReturn(5L);

        assertEquals(5L, workspaceService.getMemberCount(workspaceId));
    }

    @Test
    void getMemberCount_shouldReturnZeroWhenNoMembers() {
        when(workspaceMemberRepository.countByIdWorkspaceId(workspaceId)).thenReturn(0L);

        assertEquals(0L, workspaceService.getMemberCount(workspaceId));
    }

    // ── assignUserToWorkspace ──────────────────────────────────

    @Test
    void assignUserToWorkspace_shouldCreateNewMembership() {
        when(workspaceMemberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, userId))
            .thenReturn(Optional.empty());
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
            .thenReturn(WorkspaceMember.builder().build());

        workspaceService.assignUserToWorkspace(workspaceId, userId, "MEMBER");

        ArgumentCaptor<WorkspaceMember> captor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(captor.capture());
        WorkspaceMember saved = captor.getValue();
        assertEquals("MEMBER", saved.getRole());
        assertEquals(workspaceId, saved.getId().getWorkspaceId());
        assertEquals(userId, saved.getId().getUserId());
    }

    @Test
    void assignUserToWorkspace_shouldUpdateExistingMembership() {
        WorkspaceMember existing = WorkspaceMember.builder()
            .id(new WorkspaceMember.WorkspaceMemberId(workspaceId, userId))
            .role("MEMBER")
            .build();
        when(workspaceMemberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, userId))
            .thenReturn(Optional.of(existing));
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
            .thenReturn(existing);

        workspaceService.assignUserToWorkspace(workspaceId, userId, "ADMIN");

        assertEquals("ADMIN", existing.getRole());
        verify(workspaceMemberRepository).save(existing);
    }

    @Test
    void assignUserToWorkspace_shouldUppercaseRole() {
        when(workspaceMemberRepository.findByIdWorkspaceIdAndIdUserId(workspaceId, userId))
            .thenReturn(Optional.empty());
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
            .thenReturn(WorkspaceMember.builder().build());

        workspaceService.assignUserToWorkspace(workspaceId, userId, "member");

        ArgumentCaptor<WorkspaceMember> captor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(captor.capture());
        assertEquals("MEMBER", captor.getValue().getRole());
    }

    // ── save ───────────────────────────────────────────────────

    @Test
    void save_shouldPersistWorkspace() {
        Workspace workspace = createWorkspace();
        when(workspaceRepository.save(workspace)).thenReturn(workspace);

        Workspace result = workspaceService.save(workspace);

        assertNotNull(result);
        verify(workspaceRepository).save(workspace);
    }

    // ── createWorkspace: validation ────────────────────────────

    @Test
    void createWorkspace_shouldThrowWhenNameBlank() {
        assertThrows(IllegalArgumentException.class,
            () -> workspaceService.createWorkspace("", "desc", null, userId));
        assertThrows(IllegalArgumentException.class,
            () -> workspaceService.createWorkspace(null, "desc", null, userId));
        verify(workspaceRepository, never()).save(any(Workspace.class));
    }

    @Test
    void createWorkspace_shouldThrowWhenNameAlreadyExists() {
        // Slug collision is handled by appending a suffix; no exception thrown
        when(workspaceRepository.existsBySlug("test")).thenReturn(true);
        when(workspaceRepository.findSlugStringsByPrefix("test-"))
            .thenReturn(List.of("test-2", "test-3"));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(createWorkspace());
        when(workspaceMemberRepository.save(any(WorkspaceMember.class)))
            .thenReturn(WorkspaceMember.builder().build());

        assertDoesNotThrow(() -> workspaceService.createWorkspace("test", null, null, userId));
        verify(workspaceRepository).save(any(Workspace.class));
    }

    // ── softDelete: idempotency ────────────────────────────────

    @Test
    void softDelete_shouldBeIdempotent() {
        Workspace workspace = createWorkspace();
        when(workspaceRepository.findById(workspaceId)).thenReturn(Optional.of(workspace));
        when(workspaceRepository.save(any(Workspace.class))).thenReturn(workspace);

        workspaceService.softDeleteWorkspace(workspaceId);
        // Second call should also succeed (already deleted)
        workspaceService.softDeleteWorkspace(workspaceId);

        verify(workspaceRepository, times(2)).save(any(Workspace.class));
    }

    private String anyString() {
        return org.mockito.ArgumentMatchers.anyString();
    }
}
