package com.shiftleft.hub.document.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class DocumentParserServiceTest {

    private final DocumentParserService parser = new DocumentParserService();

    @TempDir
    Path tempDir;

    // ── text/markdown ───────────────────────────────────────────

    @Test
    void parse_shouldReturnContentForMarkdown() throws IOException {
        Path file = tempDir.resolve("test.md");
        String expected = "# Hello\n\nThis is **markdown** content.";
        Files.writeString(file, expected);

        String result = parser.parse(file, "text/markdown");

        assertEquals(expected, result);
    }

    @Test
    void parse_shouldReturnContentForMarkdownWithArbitraryExtension() throws IOException {
        // MIME type is what matters, not the file extension
        Path file = tempDir.resolve("readme.txt");
        String expected = "# Markdown content in a .txt file";
        Files.writeString(file, expected);

        String result = parser.parse(file, "text/markdown");

        assertEquals(expected, result);
    }

    // ── text/plain ──────────────────────────────────────────────

    @Test
    void parse_shouldReturnContentForPlainText() throws IOException {
        Path file = tempDir.resolve("notes.txt");
        String expected = "Plain text content\nwith multiple lines.";
        Files.writeString(file, expected);

        String result = parser.parse(file, "text/plain");

        assertEquals(expected, result);
    }

    @Test
    void parse_shouldReturnContentForPlainTextWithUtf8() throws IOException {
        Path file = tempDir.resolve("utf8.txt");
        String expected = "Contenu en français avec des accents: éàûö";
        Files.writeString(file, expected);

        String result = parser.parse(file, "text/plain");

        assertEquals(expected, result);
    }

    // ── Unsupported MIME type ───────────────────────────────────

    @Test
    void parse_shouldThrowForUnsupportedMimeType() throws IOException {
        Path file = tempDir.resolve("image.png");
        Files.writeString(file, "not an image");

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> parser.parse(file, "image/png"));

        assertTrue(ex.getMessage().contains("Unsupported MIME type"));
        assertTrue(ex.getMessage().contains("image/png"));
    }

    @Test
    void parse_shouldThrowForNullMimeType() throws IOException {
        Path file = tempDir.resolve("data.bin");
        Files.writeString(file, "binary data");

        assertThrows(NullPointerException.class,
            () -> parser.parse(file, null));
    }

    // ── PDF parsing ─────────────────────────────────────────────

    @Test
    void parse_shouldExtractTextFromPdf() throws IOException {
        Path file = tempDir.resolve("document.pdf");
        // Create a minimal byte stream that resembles PDF with text in parentheses
        // The PDF parser looks for text between ( and ) characters
        String pdfContent = """
            %PDF-1.4
            1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
            2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
            3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]
            /Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj
            4 0 obj<</Length 44>>stream
            BT /F1 12 Tf 100 700 Td (Hello World) Tj ET
            endstream
            endobj
            5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj
            xref
            0 6
            0000000000 65535 f 
            0000000009 00000 n 
            0000000058 00000 n 
            0000000115 00000 n 
            0000000266 00000 n 
            0000000362 00000 n 
            trailer
            <</Size 6/Root 1 0 R>>
            startxref
            417
            %%EOF""";
        Files.writeString(file, pdfContent);

        String result = parser.parse(file, "application/pdf");

        assertTrue(result.contains("Hello World"),
            "PDF text extraction should find text between parentheses");
    }

    @Test
    void parse_shouldFallbackToRawContentWhenPdfHasNoExtractableText() throws IOException {
        Path file = tempDir.resolve("empty.pdf");
        // PDF bytes with no parenthesized text
        String pdfContent = """
            %PDF-1.4
            1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
            2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
            3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]>>endobj
            xref
            0 4
            0000000000 65535 f 
            0000000009 00000 n 
            0000000058 00000 n 
            0000000115 00000 n 
            trailer
            <</Size 4/Root 1 0 R>>
            startxref
            188
            %%EOF""";
        Files.writeString(file, pdfContent);

        String result = parser.parse(file, "application/pdf");

        // No text in parentheses → falls back to raw content
        assertFalse(result.isEmpty(), "Fallback should return raw PDF content");
        assertTrue(result.contains("%PDF-1.4"), "Raw content should include PDF header");
    }

    @Test
    void parse_shouldNotCrashOnEscapedParentheses() throws IOException {
        Path file = tempDir.resolve("escaped.pdf");
        String pdfContent = """
            %PDF-1.4
            1 0 obj<</Type/Catalog/Pages 2 0 R>>endobj
            2 0 obj<</Type/Pages/Kids[3 0 R]/Count 1>>endobj
            3 0 obj<</Type/Page/Parent 2 0 R/MediaBox[0 0 612 792]
            /Contents 4 0 R/Resources<</Font<</F1 5 0 R>>>>>>endobj
            4 0 obj<</Length 64>>stream
            BT /F1 12 Tf 100 700 Td (\\(parentheses\\) in text) Tj ET
            endstream
            endobj
            5 0 obj<</Type/Font/Subtype/Type1/BaseFont/Helvetica>>endobj
            xref
            0 6
            0000000009 00000 n 
            0000000058 00000 n 
            0000000115 00000 n 
            0000000266 00000 n 
            0000000362 00000 n 
            0000000458 00000 n 
            trailer
            <</Size 6/Root 1 0 R>>
            startxref
            513
            %%EOF""";
        Files.writeString(file, pdfContent);

        String result = parser.parse(file, "application/pdf");

        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    // ── IOException handling ────────────────────────────────────

    @Test
    void parse_shouldWrapIoExceptionInRuntimeException() {
        Path nonExistent = tempDir.resolve("does-not-exist.md");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> parser.parse(nonExistent, "text/plain"));

        assertTrue(ex.getMessage().contains("Failed to parse document"));
        assertTrue(ex.getCause() instanceof IOException);
    }

    @Test
    void parse_shouldWrapIoExceptionForPdf() {
        Path nonExistent = tempDir.resolve("ghost.pdf");

        RuntimeException ex = assertThrows(RuntimeException.class,
            () -> parser.parse(nonExistent, "application/pdf"));

        assertTrue(ex.getMessage().contains("Failed to parse document"));
        assertTrue(ex.getCause() instanceof IOException);
    }
}
