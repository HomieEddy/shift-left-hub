package com.shiftleft.hub.ai.api.dto;

import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;

public record StreamEvent(
    String type,
    String content,
    @Nullable List<SourceRef> sources
) {
    public record SourceRef(UUID articleId, String title, String slug, double score) {}
}
