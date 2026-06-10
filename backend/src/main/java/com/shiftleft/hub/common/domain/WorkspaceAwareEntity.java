package com.shiftleft.hub.common.domain;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.annotations.Filter;
import org.hibernate.annotations.FilterDef;
import org.hibernate.annotations.ParamDef;

import java.util.UUID;

/**
 * Base class for all entities scoped to a workspace.
 * Provides a workspace_id column and Hibernate @FilterDef/@Filter annotations
 * for automatic workspace-level row-level security at the ORM layer.
 */
@MappedSuperclass
@FilterDef(name = "workspaceFilter",
    parameters = @ParamDef(name = "workspaceId", type = org.hibernate.type.descriptor.java.UUIDJavaType.class))
@Filter(name = "workspaceFilter", condition = "workspace_id = :workspaceId")
public abstract class WorkspaceAwareEntity {

    @Column(name = "workspace_id", nullable = false, updatable = false)
    private UUID workspaceId;

    /**
     * Returns the workspace ID this entity belongs to.
     *
     * @return the workspace UUID
     */
    public UUID getWorkspaceId() {
        return workspaceId;
    }

    /**
     * Sets the workspace ID this entity belongs to.
     *
     * @param workspaceId the workspace UUID
     */
    public void setWorkspaceId(UUID workspaceId) {
        this.workspaceId = workspaceId;
    }
}
