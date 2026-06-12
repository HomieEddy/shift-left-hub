package com.shiftleft.hub.document.domain;

import java.util.UUID;

public record DocumentEmbeddedEvent(UUID documentId, UUID workspaceId, int chunkCount) {
}
