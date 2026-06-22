package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

/**
 * Builds the LLM prompt for KCS article drafting.
 *
 * <p>Single responsibility: turn a {@link TicketResolvedEvent} into the
 * structured prompt that the LLM consumes. Kept separate from
 * {@link KcsDraftingService} so the prompt format can be unit-tested
 * without the LLM call, and so a future prompt revision only touches
 * one class.
 */
@Component
public class KcsPromptBuilder {

    /**
     * Returns the prompt body, formatted with the ticket's fields.
     *
     * @param event the resolved ticket event
     * @return the LLM prompt (no I/O, no side effects)
     */
    public String buildDraftingPrompt(TicketResolvedEvent event) {
        return """
You are a Knowledge-Centered Service (KCS) content specialist. \
Create a knowledge base article from a resolved IT support ticket.

## Source Ticket Information
- Ticket Number: %s
- Issue: %s
- Category: %s
- Urgency: %s
- Resolution Notes: %s
- Agent: %s
- User: %s

## Instructions
Create a complete, bilingual knowledge base article in the following format.
Return ONLY the structured fields below — no preamble, no commentary, no markdown code fences.

title_en: <English title, concise and descriptive, max 80 chars>
title_fr: <French translation of the title>
excerpt: <One-sentence summary of the solution in English, max 160 chars>
content_en:
<Full English article content in markdown format. Structure:

## Overview
Brief description of the issue.

## Steps to Resolve
1. Step one
2. Step two
3. Step three

## Notes
Any additional context, prerequisites, or warnings.

>
content_fr:
<Full French translation of the article content, preserving markdown structure.>
suggested_tags: <Comma-separated list of suggested tag names in English>
""".formatted(
                event.ticketNumber(),
                event.issue(),
                event.category(),
                event.urgency(),
                Objects.toString(event.resolutionNotes(), "N/A"),
                event.agentDisplayName(),
                event.userDisplayName()
            );
    }
}
