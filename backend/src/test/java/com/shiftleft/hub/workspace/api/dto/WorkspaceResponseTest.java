package com.shiftleft.hub.workspace.api.dto;

import com.shiftleft.hub.workspace.domain.Workspace;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorkspaceResponseTest {

    @Test
    void from_builds_response_with_provided_member_count() throws Exception {
        Workspace workspace = new Workspace();
        UUID id = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        setField(workspace, "id", id);
        setField(workspace, "name", "Acme");
        setField(workspace, "slug", "acme");
        setField(workspace, "description", "test");
        setField(workspace, "logoUrl", "https://example.com/logo.png");
        setField(workspace, "icon", "building");
        setField(workspace, "createdBy", createdBy);
        setField(workspace, "createdAt", now);
        setField(workspace, "updatedAt", now);

        WorkspaceResponse response = WorkspaceResponse.from(workspace, 42L);

        assertEquals(id, response.id());
        assertEquals("Acme", response.name());
        assertEquals("acme", response.slug());
        assertEquals("test", response.description());
        assertEquals("https://example.com/logo.png", response.logoUrl());
        assertEquals("building", response.icon());
        assertEquals(createdBy, response.createdBy());
        assertEquals(42L, response.memberCount());
        assertEquals(now, response.createdAt());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(target, value);
    }
}
