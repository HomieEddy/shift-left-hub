package com.shiftleft.hub.common.config;

import com.shiftleft.hub.common.config.seeder.AiConfigSeeder;
import com.shiftleft.hub.common.config.seeder.OldArticleCleanupSeeder;
import com.shiftleft.hub.common.config.seeder.UserSeeder;
import com.shiftleft.hub.common.config.seeder.WorkspaceAssignmentSeeder;
import com.shiftleft.hub.common.config.seeder.WorkspaceSeeder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MasterSeederTest {

    @Mock private UserSeeder userSeeder;
    @Mock private WorkspaceSeeder workspaceSeeder;
    @Mock private WorkspaceAssignmentSeeder workspaceAssignmentSeeder;
    @Mock private AiConfigSeeder aiConfigSeeder;
    @Mock private OldArticleCleanupSeeder oldArticleCleanupSeeder;

    private MasterSeeder seeder;

    @BeforeEach
    void setUp() throws Exception {
        seeder = new MasterSeeder(userSeeder, workspaceSeeder, workspaceAssignmentSeeder,
            aiConfigSeeder, oldArticleCleanupSeeder);
        setField("adminEmail", "admin@company.com");
        setField("adminPassword", "test-password");
    }

    private void setField(String name, String value) throws Exception {
        Field field = MasterSeeder.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(seeder, value);
    }

    @Test
    void seed_shouldInvokeAllSeedersInOrder() {
        seeder.seed();

        verify(userSeeder).seedUsers("admin@company.com", "test-password");
        verify(workspaceSeeder).seedWorkspaces(any());
        verify(workspaceAssignmentSeeder).assignUsersAndSetDefaults("admin@company.com");
        verify(aiConfigSeeder).seedAiConfig();
        verify(oldArticleCleanupSeeder).cleanupOldArticles();
    }

    @Test
    void seed_shouldSkipWhenAdminEmailIsNull() throws Exception {
        setField("adminEmail", null);

        seeder.seed();

        verifyNoInteractions(userSeeder, workspaceSeeder, workspaceAssignmentSeeder,
            aiConfigSeeder, oldArticleCleanupSeeder);
    }

    @Test
    void seed_shouldSkipWhenAdminPasswordIsNull() throws Exception {
        setField("adminPassword", null);

        seeder.seed();

        verifyNoInteractions(userSeeder, workspaceSeeder, workspaceAssignmentSeeder,
            aiConfigSeeder, oldArticleCleanupSeeder);
    }

    @Test
    void seed_shouldContinueWhenSeederThrows() {
        when(userSeeder.seedUsers(anyString(), anyString())).thenThrow(new RuntimeException("boom"));

        assertDoesNotThrow(() -> seeder.seed());

        verify(workspaceSeeder, never()).seedWorkspaces(any());
        verify(aiConfigSeeder, never()).seedAiConfig();
    }

    @Test
    void seed_shouldPassAdminUserToWorkspaceSeeder() {
        when(userSeeder.seedUsers(anyString(), anyString())).thenReturn(null);

        seeder.seed();

        verify(workspaceSeeder).seedWorkspaces(isNull());
    }
}