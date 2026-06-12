package com.shiftleft.hub.document.domain;

import java.util.UUID;

public record DocumentUploadedEvent(UUID documentId, UUID workspaceId) {
}
