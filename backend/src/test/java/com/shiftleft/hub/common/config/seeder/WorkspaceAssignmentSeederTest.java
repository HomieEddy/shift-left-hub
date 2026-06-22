package com.shiftleft.hub.common.config.seeder;

import com.shiftleft.hub.user.domain.User;
import com.shiftleft.hub.user.domain.UserRepository;
import com.shiftleft.hub.workspace.domain.Workspace;
import com.shiftleft.hub.workspace.service.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkspaceAssignmentSeederTest {

    @Mock private WorkspaceService workspaceService;
    @Mock private UserRepository userRepository;

    private WorkspaceAssignmentSeeder seeder;
    private final UUID adminId = UUID.randomUUID();
    private final UUID publicWsId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        seeder = new WorkspaceAssignmentSeeder(workspaceService, userRepository);
    }

    @Test
    void assignUsersAndSetDefaults_assignsAdminToAllWorkspaces() {
        User admin = User.builder().id(adminId).email("admin@x.com").build();
        for (var seed : WorkspaceSeeder.workspaceSeeds()) {
            when(workspaceService.findBySlug(seed.slug()))
                .thenReturn(Optional.of(Workspace.builder().id(publicWsId).slug(seed.slug()).build()));
        }
        when(userRepository.findByEmail("admin@x.com")).thenReturn(Optional.of(admin));

        seeder.assignUsersAndSetDefaults("admin@x.com");

        verify(workspaceService, atLeast(1)).assignUserToWorkspace(any(), eq(adminId), eq("ADMIN"));
    }

    @Test
    void assignUsersAndSetDefaults_skipsWhenPublicWorkspaceMissing() {
        when(workspaceService.findBySlug(anyString())).thenReturn(Optional.empty());

        seeder.assignUsersAndSetDefaults("admin@x.com");

        verify(workspaceService, never()).assignUserToWorkspace(any(), any(), anyString());
    }

    @Test
    void assignUsersAndSetDefaults_handlesNullAdminEmail() {
        for (var seed : WorkspaceSeeder.workspaceSeeds()) {
            when(workspaceService.findBySlug(seed.slug()))
                .thenReturn(Optional.of(Workspace.builder().id(publicWsId).slug(seed.slug()).build()));
        }
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertDoesNotThrow(() -> seeder.assignUsersAndSetDefaults("missing@x.com"));
    }
}