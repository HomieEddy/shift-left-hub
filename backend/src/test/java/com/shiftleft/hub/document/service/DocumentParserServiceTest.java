package com.shiftleft.hub.document.service;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.FileOutputStream;
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

    // ── HTML parsing (Jsoup) ─────────────────────────────────────

    @Test
    void parseHtml_shouldExtractTextStrippingMarkup() throws IOException {
        Path file = tempDir.resolve("test.html");
        String html = """
            <html><body>
            <h1>Heading One</h1>
            <p>This is a paragraph with <strong>bold</strong> and <em>italic</em> text.</p>
            <ul>
              <li>List item A</li>
              <li>List item B</li>
            </ul>
            </body></html>""";
        Files.writeString(file, html);

        String result = parser.parse(file, "text/html");

        assertTrue(result.contains("Heading One"), "Should contain heading text");
        assertTrue(result.contains("This is a paragraph with"), "Should contain paragraph text");
        assertTrue(result.contains("bold"), "Should contain bold text content");
        assertTrue(result.contains("italic"), "Should contain italic text content");
        assertTrue(result.contains("List item A"), "Should contain list item text");
        assertTrue(result.contains("List item B"), "Should contain list item text");
        assertFalse(result.contains("<h1>"), "Should not contain HTML tags");
        assertFalse(result.contains("<strong>"), "Should not contain strong tags");
    }

    @Test
    void parseHtml_shouldStripScriptAndStyleContent() throws IOException {
        Path file = tempDir.resolve("scripted.html");
        String html = """
            <html><head>
            <style>.css-rules { color: red; }</style>
            </head><body>
            <p>Visible content</p>
            <script>alert('injected');</script>
            <p>More visible content</p>
            </body></html>""";
        Files.writeString(file, html);

        String result = parser.parse(file, "text/html");

        assertTrue(result.contains("Visible content"), "Should contain visible content");
        assertTrue(result.contains("More visible content"), "Should contain visible content");
        assertFalse(result.contains("css-rules"), "Should not contain CSS from style tags");
        assertFalse(result.contains("alert"), "Should not contain JavaScript from script tags");
    }

    @Test
    void parseHtml_shouldHandleEmptyHtml() throws IOException {
        Path file = tempDir.resolve("empty.html");
        Files.writeString(file, "<html><body></body></html>");

        String result = parser.parse(file, "text/html");

        assertTrue(result == null || result.isBlank(), "Empty HTML should produce blank result");
    }

    @Test
    void parseHtml_shouldHandleXhtml() throws IOException {
        Path file = tempDir.resolve("test.xhtml");
        String xhtml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head><title>XHTML Test</title></head>
            <body>
            <p>XHTML content with <br/> line break.</p>
            </body>
            </html>""";
        Files.writeString(file, xhtml);

        String result = parser.parse(file, "application/xhtml+xml");

        assertTrue(result.contains("XHTML content"), "Should extract XHTML text");
    }

    // ── XML parsing (Jsoup XML mode) ────────────────────────────

    @Test
    void parseXml_shouldExtractTextFromElements() throws IOException {
        Path file = tempDir.resolve("test.xml");
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <catalog>
                <book>
                    <title>Effective Java</title>
                    <author>Joshua Bloch</author>
                    <description>A programming guide.</description>
                </book>
            </catalog>""";
        Files.writeString(file, xml);

        String result = parser.parse(file, "text/xml");

        assertTrue(result.contains("Effective Java"), "Should extract title text");
        assertTrue(result.contains("Joshua Bloch"), "Should extract author text");
        assertTrue(result.contains("A programming guide."), "Should extract description text");
    }

    @Test
    void parseXml_shouldStripProcessingInstructions() throws IOException {
        Path file = tempDir.resolve("with-pi.xml");
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <?xml-stylesheet type="text/xsl" href="style.xsl"?>
            <!DOCTYPE catalog SYSTEM "catalog.dtd">
            <catalog>
                <item>Content</item>
            </catalog>""";
        Files.writeString(file, xml);

        String result = parser.parse(file, "application/xml");

        assertTrue(result.contains("Content"), "Should extract element text");
        assertFalse(result.contains("<?xml"), "Should not contain XML declaration");
    }

    @Test
    void parseXml_shouldHandleDeeplyNestedXml() throws IOException {
        Path file = tempDir.resolve("deep.xml");
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <root>
                <level1>
                    <level2>
                        <level3>Deep text</level3>
                    </level2>
                </level1>
                <sibling>Flat text</sibling>
            </root>""";
        Files.writeString(file, xml);

        String result = parser.parse(file, "application/rss+xml");

        assertTrue(result.contains("Deep text"), "Should extract deeply nested text");
        assertTrue(result.contains("Flat text"), "Should extract sibling element text");
    }

    @Test
    void parseXml_shouldReturnRawContentWhenNoText() throws IOException {
        Path file = tempDir.resolve("no-text.xml");
        String xml = """
            <?xml version="1.0" encoding="UTF-8"?>
            <empty><items></items></empty>""";
        Files.writeString(file, xml);

        String result = parser.parse(file, "application/atom+xml");

        assertFalse(result.isEmpty(), "Should return fallback content");
    }

    @Test
    void parseXml_shouldHandleEmptyXml() {
        assertDoesNotThrow(() -> {
            Path file = tempDir.resolve("empty.xml");
            Files.writeString(file, "");
            String result = parser.parse(file, "text/xml");
            assertNotNull(result);
        }, "Empty XML should not throw exception");
    }

    // ── Word document parsing (Apache POI) ──────────────────────

    @Test
    void parseDocx_shouldExtractText() throws IOException {
        Path file = tempDir.resolve("test.docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            doc.createParagraph().createRun().setText("Hello from Word");
            doc.createParagraph().createRun().setText("Second paragraph");
            try (FileOutputStream out = new FileOutputStream(file.toFile())) {
                doc.write(out);
            }
        }

        String result = parser.parse(file,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertTrue(result.contains("Hello from Word"), "Should extract first paragraph");
        assertTrue(result.contains("Second paragraph"), "Should extract second paragraph");
    }

    @Test
    void parseDocx_shouldFallbackOnEmptyDocx() throws IOException {
        Path file = tempDir.resolve("empty.docx");
        try (XWPFDocument doc = new XWPFDocument()) {
            try (FileOutputStream out = new FileOutputStream(file.toFile())) {
                doc.write(out);
            }
        }

        String result = parser.parse(file,
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document");

        assertFalse(result.isEmpty(), "Should return fallback content");
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
