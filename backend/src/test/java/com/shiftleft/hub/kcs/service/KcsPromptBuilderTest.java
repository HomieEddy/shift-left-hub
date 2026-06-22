package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import com.shiftleft.hub.ticket.domain.TicketCategory;
import com.shiftleft.hub.ticket.domain.TicketUrgency;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class KcsPromptBuilderTest {

    private final KcsPromptBuilder builder = new KcsPromptBuilder();

    private TicketResolvedEvent event() {
        return new TicketResolvedEvent(
            UUID.randomUUID(), "TKT-0042",
            "Cannot connect to VPN",
            "{\"chat\":\"...\"}",
            TicketCategory.NETWORK, TicketUrgency.HIGH,
            "Reset credentials and reconfigured client",
            "John Doe", "john@example.com",
            "Agent Smith", LocalDateTime.now()
        );
    }

    @Test
    void buildDraftingPrompt_includesAllTicketFields() {
        String prompt = builder.buildDraftingPrompt(event());

        assertTrue(prompt.contains("TKT-0042"));
        assertTrue(prompt.contains("Cannot connect to VPN"));
        assertTrue(prompt.contains("NETWORK"));
        assertTrue(prompt.contains("HIGH"));
        assertTrue(prompt.contains("Reset credentials and reconfigured client"));
        assertTrue(prompt.contains("John Doe"));
        assertTrue(prompt.contains("Agent Smith"));
    }

    @Test
    void buildDraftingPrompt_usesN_AForNullResolutionNotes() {
        TicketResolvedEvent ev = new TicketResolvedEvent(
            UUID.randomUUID(), "TKT-0042",
            "Issue", null,
            TicketCategory.NETWORK, TicketUrgency.LOW,
            null, null, null,
            null, LocalDateTime.now()
        );

        String prompt = builder.buildDraftingPrompt(ev);

        assertTrue(prompt.contains("Resolution Notes: N/A"));
    }

    @Test
    void buildDraftingPrompt_includesAllRequiredFieldMarkers() {
        String prompt = builder.buildDraftingPrompt(event());

        // Verify all required LLM output field markers are documented in the prompt
        assertTrue(prompt.contains("title_en:"));
        assertTrue(prompt.contains("title_fr:"));
        assertTrue(prompt.contains("excerpt:"));
        assertTrue(prompt.contains("content_en:"));
        assertTrue(prompt.contains("content_fr:"));
        assertTrue(prompt.contains("suggested_tags:"));
    }
}
