package com.shiftleft.hub.document.domain;

import java.util.UUID;

public record DocumentParsedEvent(UUID documentId, UUID workspaceId, String content) {
}
