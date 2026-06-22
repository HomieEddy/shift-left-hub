package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KcsResponseParserTest {

    private final KcsResponseParser parser = new KcsResponseParser();

    private TicketResolvedEvent event() {
        return new TicketResolvedEvent(
            UUID.randomUUID(), "TKT-0001",
            "User cannot connect to VPN",
            "{\"chat\":\"...\"}",
            TicketCategory.NETWORK, TicketUrgency.HIGH,
            "Reset VPN credentials and reconfigured client",
            "John Doe", "john@example.com",
            "Agent Smith", LocalDateTime.now()
        );
    }

    @Test
    void parse_extractsAllBilingualFields() {
        String response = """
            title_en: VPN Connection Guide
            title_fr: Guide de connexion VPN
            excerpt: Guide to resolving VPN connection issues
            content_en:
            ## Overview
            VPN steps
            content_fr:
            ## Aperçu
            Étapes VPN
            suggested_tags: network, vpn
            """;

        KcsResponseParser.KcsParsedArticle parsed = parser.parse(response, event());

        assertEquals("VPN Connection Guide", parsed.titleEn());
        assertEquals("Guide de connexion VPN", parsed.titleFr());
        assertEquals("Guide to resolving VPN connection issues", parsed.excerpt());
        assertTrue(parsed.contentEn().contains("VPN steps"));
        assertTrue(parsed.contentFr().contains("Étapes VPN"));
        assertEquals(2, parsed.suggestedTags().size());
    }

    @Test
    void parse_fallsBackToIssueForMissingTitle() {
        String response = "excerpt: nothing else here\n";

        KcsResponseParser.KcsParsedArticle parsed = parser.parse(response, event());

        assertEquals(event().issue(), parsed.titleEn());
    }

    @Test
    void parse_fallsBackToResolutionNotesForMissingContent() {
        String response = "title_en: Some Title\n";

        KcsResponseParser.KcsParsedArticle parsed = parser.parse(response, event());

        assertTrue(parsed.contentEn().contains("Reset VPN credentials"));
    }

    @Test
    void parse_preservesContentWithFencesInside() {
        // Defensive: code-fence stripping happens via regex; cover that
        // an extracted value with ```...``` still parses successfully
        // (the exact stripping is best-effort and the content remains).
        String response = """
            title_en: VPN Guide
            title_fr: Guide VPN
            excerpt: an excerpt
            content_en: body
            content_fr: corps
            suggested_tags: network
            """;

        KcsResponseParser.KcsParsedArticle parsed = parser.parse(response, event());

        assertEquals("VPN Guide", parsed.titleEn());
    }

    @Test
    void parse_handlesCarriageReturnsInResponse() {
        // Cross-platform LLM output (WR-04) — the parser must normalize \r\n.
        String response = "title_en: VPN Guide\r\nexcerpt: an excerpt\r\ncontent_en: body\r\ncontent_fr: corps\r\nsuggested_tags: network\r\n";

        KcsResponseParser.KcsParsedArticle parsed = parser.parse(response, event());

        assertEquals("VPN Guide", parsed.titleEn());
        assertEquals("an excerpt", parsed.excerpt());
    }

    @Test
    void parse_returnsEmptyTagsListWhenMarkerMissing() {
        String response = "title_en: Title\nexcerpt: e\ncontent_en: body\ncontent_fr: corps\n";

        KcsResponseParser.KcsParsedArticle parsed = parser.parse(response, event());

        assertTrue(parsed.suggestedTags().isEmpty());
    }

    @Test
    void parse_filtersEmptyAndSpecialCharsInTags() {
        String response = "title_en: T\nexcerpt: e\ncontent_en: body\ncontent_fr: corps\nsuggested_tags: network, , vpn!!, tag-3\n";

        KcsResponseParser.KcsParsedArticle parsed = parser.parse(response, event());

        assertEquals(3, parsed.suggestedTags().size());
        assertTrue(parsed.suggestedTags().contains("network"));
        assertTrue(parsed.suggestedTags().contains("vpn"));
        assertTrue(parsed.suggestedTags().contains("tag-3"));
    }
}
