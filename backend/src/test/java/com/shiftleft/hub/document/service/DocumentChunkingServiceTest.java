package com.shiftleft.hub.document.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentChunkingServiceTest {

    private DocumentChunkingService service;

    @BeforeEach
    void setUp() {
        service = new DocumentChunkingService(10, 0);
    }

    // ── Edge: null / empty content ──────────────────────────────

    @Test
    void chunk_shouldReturnEmptyListForNullContent() {
        List<String> result = service.chunk(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void chunk_shouldReturnEmptyListForBlankContent() {
        assertTrue(service.chunk("").isEmpty());
        assertTrue(service.chunk("   ").isEmpty());
        assertTrue(service.chunk("\t\n  \n").isEmpty());
    }

    // ── Short content ───────────────────────────────────────────

    @Test
    void chunk_shouldReturnSingleChunkForShortContent() {
        String content = "Hello, this is a short document.";
        List<String> chunks = service.chunk(content);

        assertEquals(1, chunks.size());
        assertEquals(content.trim(), chunks.getFirst());
    }

    @Test
    void chunk_shouldReturnSingleChunkWhenContentFitsExactly() {
        // With maxTokens=10, chunkSize=40 chars
        String content = "A".repeat(40);
        List<String> chunks = service.chunk(content);

        assertEquals(1, chunks.size());
        assertEquals(content, chunks.getFirst());
    }

    // ── Long content — multiple chunks ──────────────────────────

    @Test
    void chunk_shouldSplitLongContentIntoMultipleChunks() {
        // With maxTokens=10, chunkSize=40: 100 chars → 3 chunks (40+40+20)
        String content = "A".repeat(100);
        List<String> chunks = service.chunk(content);

        assertEquals(3, chunks.size());
        assertTrue(chunks.get(0).length() <= 40);
        assertTrue(chunks.get(1).length() <= 40);
    }

    @Test
    void chunk_shouldProduceContiguousNonOverlappingContent() {
        String content = "A".repeat(100);
        List<String> chunks = service.chunk(content);

        assertFalse(chunks.isEmpty());
        int totalLength = chunks.stream().mapToInt(String::length).sum();
        assertEquals(100, totalLength, "Total length should equal original when overlap=0");
    }

    // ── Paragraph boundary detection ────────────────────────────

    @Test
    void chunk_shouldBreakAtParagraphBoundary() {
        String paraBreak = "A".repeat(30) + "\n\n" + "B".repeat(38);
        List<String> chunks = service.chunk(paraBreak);

        assertEquals(2, chunks.size(), "Should create two chunks split at paragraph break");
        assertTrue(chunks.get(0).matches("A+"), "First chunk should contain only A's");
        assertTrue(chunks.get(1).matches("B+"), "Second chunk should contain only B's");
    }

    @Test
    void chunk_shouldBreakAtSentenceBoundaryWhenNoParagraphBreak() {
        String content = "A".repeat(25) + ".\n" + "B".repeat(60);
        List<String> chunks = service.chunk(content);

        assertEquals(2, chunks.size(), "Should split at sentence boundary");
        assertTrue(chunks.get(0).matches("A*\\.?"), "First chunk should end at sentence boundary");
    }

    @Test
    void chunk_shouldBreakAtSpaceWhenNoSentenceBreak() {
        String content = "A".repeat(35) + " " + "B".repeat(60);
        List<String> chunks = service.chunk(content);

        assertEquals(2, chunks.size(), "Should split at space boundary");
        assertTrue(chunks.get(1).matches("B+"), "Second chunk should contain only B's");
    }

    @Test
    void chunk_shouldBreakAtHardBoundaryWhenNoGoodBreakFound() {
        // Content with no paragraph, sentence, or space breaks near boundary
        // All characters are non-breaking within search range
        String content = "AAAA".repeat(25); // 100 chars, no breaks at all
        List<String> chunks = service.chunk(content);

        assertTrue(chunks.size() >= 2, "Should split at hard character boundary");
        for (String chunk : chunks) {
            assertFalse(chunk.isEmpty(), "Each chunk should be non-empty");
        }
    }

    @Test
    void chunk_shouldTrimWhitespaceFromChunks() {
        String content = "  Leading and trailing whitespace  ".repeat(5);
        // Trigger multi-chunk scenario
        List<String> chunks = service.chunk(content);

        for (String chunk : chunks) {
            assertEquals(chunk, chunk.trim(), "Each chunk should be trimmed");
        }
    }
}
