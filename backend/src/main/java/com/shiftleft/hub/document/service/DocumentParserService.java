package com.shiftleft.hub.document.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@Slf4j
public class DocumentParserService {

    /**
     * Parses a document file and extracts its text content.
     * Supports markdown, plain text, PDF, HTML, XML, and Word documents.
     *
     * @param filePath the path to the document file
     * @param mimeType the MIME type of the document
     * @return the extracted text content
     */
    public String parse(Path filePath, String mimeType) {
        try {
            return switch (mimeType) {
                case "text/markdown", "text/plain" -> Files.readString(filePath);
                case "application/pdf" -> parsePdf(filePath);
                case "text/html", "application/xhtml+xml" -> parseHtml(filePath);
                case "text/xml", "application/xml", "application/rss+xml", "application/atom+xml" -> parseXml(filePath);
                case "application/vnd.openxmlformats-officedocument.wordprocessingml.document" -> parseDocx(filePath);
                default -> throw new IllegalArgumentException("Unsupported MIME type: " + mimeType);
            };
        } catch (IOException e) {
            throw new RuntimeException("Failed to parse document: " + filePath, e);
        }
    }

    private String parsePdf(Path filePath) throws IOException {
        // Simple PDF text extraction using basic approach
        // Reads raw bytes and extracts text content
        byte[] bytes = Files.readAllBytes(filePath);
        StringBuilder text = new StringBuilder();
        // Basic PDF text extraction — look for text between parentheses in PDF streams
        String content = new String(bytes, StandardCharsets.UTF_8);
        boolean inText = false;
        for (int i = 0; i < content.length(); i++) {
            if (content.charAt(i) == '(' && (i == 0 || content.charAt(i - 1) != '\\')) {
                inText = true;
            } else if (content.charAt(i) == ')' && inText) {
                inText = false;
                text.append(' ');
            } else if (inText) {
                text.append(content.charAt(i));
            }
        }
        String result = text.toString().trim();
        if (result.isEmpty()) {
            log.warn("No text extracted from PDF, falling back to raw content");
            result = content;
        }
        return result;
    }

    private String parseHtml(Path filePath) throws IOException {
        org.jsoup.nodes.Document htmlDoc = Jsoup.parse(filePath.toFile(), "UTF-8");
        htmlDoc.select("script, style, svg, noscript").remove();
        return htmlDoc.body().text();
    }

    private String parseXml(Path filePath) throws IOException {
        String xmlContent = Files.readString(filePath);
        org.jsoup.nodes.Document xmlDoc = Jsoup.parse(xmlContent, "", Parser.xmlParser());
        String text = xmlDoc.text();
        if (text.isBlank()) {
            log.warn("No text extracted from XML, falling back to raw content for: {}", filePath);
            return xmlContent;
        }
        return text;
    }

    private String parseDocx(Path filePath) throws IOException {
        try (InputStream is = Files.newInputStream(filePath);
             XWPFDocument doc = new XWPFDocument(is);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            String text = extractor.getText();
            if (text.isBlank()) {
                log.warn("No text extracted from Word document, falling back to raw content for: {}", filePath);
                byte[] rawBytes = Files.readAllBytes(filePath);
                return new String(rawBytes, StandardCharsets.UTF_8);
            }
            return text;
        }
    }
}
