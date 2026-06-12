package com.shiftleft.hub.document.domain;

import java.util.UUID;

public record DocumentChunkedEvent(UUID documentId, UUID workspaceId, int chunkCount) {
}
