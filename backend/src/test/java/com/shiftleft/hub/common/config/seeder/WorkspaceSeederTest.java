package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRole;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.domain.WorkspaceRepository;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceSeederTest {

    @Mock private WorkspaceService workspaceService;
    @Mock private WorkspaceRepository workspaceRepository;

    private WorkspaceSeeder seeder;
    private final UUID adminId = UUID.randomUUID();
    private User admin;

    @BeforeEach
    void setUp() {
        admin = User.builder().id(adminId).email("admin@x.com").role(UserRole.ROLE_ADMIN).build();
        seeder = new WorkspaceSeeder(workspaceService, workspaceRepository);
    }

    @Test
    void seedWorkspaces_createsAllFourWorkspacesWhenAbsent() {
        when(workspaceService.findBySlug(anyString())).thenReturn(Optional.empty());
        when(workspaceService.createWorkspace(anyString(), anyString(), any(), eq(adminId)))
            .thenAnswer(inv -> Workspace.builder().id(UUID.randomUUID())
                .name(inv.getArgument(0)).build());
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        seeder.seedWorkspaces(admin);

        verify(workspaceService, times(4)).createWorkspace(anyString(), anyString(), any(), eq(adminId));
        verify(workspaceRepository, times(4)).save(any(Workspace.class));
    }

    @Test
    void seedWorkspaces_skipsAllWorkspacesWhenAllExist() {
        // Stub all 4 slugs so seedWorkspaces finds them all existing and skips
        for (var seed : WorkspaceSeeder.workspaceSeeds()) {
            when(workspaceService.findBySlug(seed.slug()))
                .thenReturn(Optional.of(Workspace.builder().id(UUID.randomUUID()).slug(seed.slug()).build()));
        }

        seeder.seedWorkspaces(admin);

        verify(workspaceService, never()).createWorkspace(anyString(), anyString(), any(), any());
        verify(workspaceRepository, never()).save(any(Workspace.class));
    }

    @Test
    void seedWorkspaces_setsIconAfterCreation() {
        when(workspaceService.findBySlug(anyString())).thenReturn(Optional.empty());
        when(workspaceService.createWorkspace(anyString(), anyString(), any(), eq(adminId)))
            .thenAnswer(inv -> Workspace.builder().id(UUID.randomUUID()).name(inv.getArgument(0)).build());
        when(workspaceRepository.save(any(Workspace.class))).thenAnswer(inv -> inv.getArgument(0));

        seeder.seedWorkspaces(admin);

        verify(workspaceRepository, times(4)).save(argThat((Workspace w) -> w.getIcon() != null));
    }

    @Test
    void workspaceSeeds_exposesFourSeedsWithPublicSlug() {
        var seeds = WorkspaceSeeder.workspaceSeeds();
        assertEquals(4, seeds.size());
        assertTrue(seeds.stream().anyMatch(s -> WorkspaceSeeder.PUBLIC_SLUG.equals(s.slug())));
    }
}