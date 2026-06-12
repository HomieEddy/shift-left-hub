package com.shiftleft.hub.document.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class DocumentChunkingService {

    private final int maxTokensPerChunk;
    private final int overlapTokens;

    @Autowired
    public DocumentChunkingService(
            @Value("${app.document.chunk.max-tokens:512}") int maxTokensPerChunk,
            @Value("${app.document.chunk.overlap-tokens:50}") int overlapTokens) {
        this.maxTokensPerChunk = maxTokensPerChunk;
        this.overlapTokens = overlapTokens;
    }

    private static final double AVG_CHARS_PER_TOKEN = 4.0;

    /**
     * Splits document content into overlapping chunks for embedding.
     * Attempts to break at paragraph or sentence boundaries when possible.
     *
     * @param content the document text content
     * @return list of chunk text strings
     */
    public List<String> chunk(String content) {
        List<String> chunks = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return chunks;
        }

        int start = 0;

        while (start < content.length()) {
            int chunkSize = (int) (maxTokensPerChunk * AVG_CHARS_PER_TOKEN);
            int end = Math.min(start + chunkSize, content.length());

            // Try to break at paragraph or sentence boundary
            if (end < content.length()) {
                int breakPoint = findBreakPoint(content, end, chunkSize / 2);
                if (breakPoint > start) {
                    end = breakPoint;
                }
            }

            chunks.add(content.substring(start, end).trim());
            int overlapSize = (int) (overlapTokens * AVG_CHARS_PER_TOKEN);
            start = end - overlapSize;

            // Avoid infinite loop for small documents
            if (start >= content.length() || start < 0) {
                break;
            }
        }

        log.debug("Chunked content into {} chunks (max {} tokens, {} overlap)",
            chunks.size(), maxTokensPerChunk, overlapTokens);
        return chunks;
    }

    private int findBreakPoint(String content, int fromPos, int searchBack) {
        int searchStart = Math.max(0, fromPos - searchBack);
        int searchEnd = Math.min(content.length(), fromPos + 50);

        // Prefer paragraph breaks
        int paraBreak = content.indexOf("\n\n", searchStart);
        if (paraBreak >= searchStart && paraBreak <= searchEnd) {
            return paraBreak + 2;
        }

        // Then sentence breaks
        int sentenceBreak = findLastIndexOfAny(content, ".!?\n", searchStart, fromPos);
        if (sentenceBreak > searchStart) {
            return sentenceBreak + 1;
        }

        // Then space
        int spaceBreak = content.lastIndexOf(' ', fromPos);
        if (spaceBreak > searchStart) {
            return spaceBreak + 1;
        }

        return fromPos;
    }

    private int findLastIndexOfAny(String content, String chars, int fromIndex, int toIndex) {
        int lastIndex = -1;
        for (char c : chars.toCharArray()) {
            int idx = content.lastIndexOf(c, toIndex);
            if (idx >= fromIndex && idx > lastIndex) {
                lastIndex = idx;
            }
        }
        return lastIndex;
    }
}
