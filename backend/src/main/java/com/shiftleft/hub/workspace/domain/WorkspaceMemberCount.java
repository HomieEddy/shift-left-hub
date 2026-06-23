package com.shiftleft.hub.workspace.domain;

import java.util.UUID;

/**
 * Projection used by {@link WorkspaceMemberRepository#countMembersByWorkspaceIds}
 * to return per-workspace member counts in a single bulk query.
 */
public interface WorkspaceMemberCount {
    UUID getWorkspaceId();

    long getMemberCount();
}
