package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * Parses the raw LLM response into structured {@link KcsParsedArticle} fields.
 *
 * <p>Single responsibility: extract a known set of field markers from
 * the LLM text and apply fallbacks for missing/blank fields. Kept
 * separate from {@link KcsDraftingService} so the parser can be
 * unit-tested against synthetic LLM outputs.
 */
@Component
public class KcsResponseParser {

    /** Internal record for parsed LLM output. */
    public record KcsParsedArticle(
        String titleEn, String titleFr,
        String contentEn, String contentFr,
        String excerpt, List<String> suggestedTags
    ) {
    }

    /**
     * Parses the LLM text. Falls back to the ticket issue / resolution notes
     * for any blank field so the resulting article is never empty.
     *
     * @param response the raw LLM text
     * @param event    the source ticket event (used for fallbacks)
     * @return the parsed fields; never null
     */
    public KcsParsedArticle parse(String response, TicketResolvedEvent event) {
        String titleEn = extractField(response, "title_en");
        String excerpt = extractField(response, "excerpt");
        String contentEn = extractField(response, "content_en");

        if (titleEn == null || titleEn.isBlank()) {
            titleEn = event.issue();
        }
        String notes = Objects.toString(event.resolutionNotes(), "");
        if (contentEn == null || contentEn.isBlank()) {
            contentEn = "Resolution: " + notes;
        }
        if (excerpt == null || excerpt.isBlank()) {
            excerpt = notes.length() > 160
                ? notes.substring(0, 157) + "..."
                : notes;
        }

        List<String> tags = parseTags(extractField(response, "suggested_tags"));

        String titleFr = extractField(response, "title_fr");
        String contentFr = extractField(response, "content_fr");
        return new KcsParsedArticle(titleEn, titleFr != null ? titleFr : titleEn,
            contentEn, contentFr != null ? contentFr : contentEn,
            excerpt, tags);
    }

    private List<String> parseTags(String tagsStr) {
        if (tagsStr == null || tagsStr.isBlank()) {
            return List.of();
        }
        return Arrays.stream(tagsStr.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> s.replaceAll("[^a-zA-Z0-9\\s-]", ""))
            .toList();
    }

    private String extractField(String response, String fieldName) {
        // Normalize line endings to handle cross-platform LLM output (WR-04)
        String normalized = response.replace("\r\n", "\n");
        String prefix = fieldName + ":";
        int start = normalized.indexOf(prefix);
        if (start == -1) {
            return null;
        }
        start += prefix.length();
        if (start < normalized.length() && normalized.charAt(start) == '\n') {
            start++;
        }

        int end = normalized.length();
        String[] markers = {"title_en:", "title_fr:", "excerpt:", "content_en:", "content_fr:", "suggested_tags:"};
        for (String marker : markers) {
            if (marker.equals(fieldName + ":")) {
                continue;
            }
            int idx = normalized.indexOf("\n" + marker, start);
            if (idx != -1 && idx < end) {
                end = idx;
            }
        }

        String value = normalized.substring(start, end).trim();
        // Remove markdown code fences if present
        return value.replaceAll("```\\w*", "").trim();
    }
}
