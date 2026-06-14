# Shift-Left Knowledge Hub - Product Requirements Document (PRD)

## 1. The Overview
The "Shift-Left Knowledge Hub" is a modern, domain-agnostic knowledge platform that turns any collection of documents into an intelligent, AI-powered assistant. Unlike traditional helpdesk systems, it provides a plug-and-play workspace model where any team — IT, HR, Legal, Product — can bring their own knowledge base and their own LLM. Workspaces define the domain, not the application.

## 2. The "Why" (Problem & Value Proposition)

### The Core Problem
Organizations accumulate vast amounts of documentation but lack intelligent, user-friendly interfaces to surface that knowledge. IT support desks are the canonical use case, but the same pattern repeats in HR (policy lookup), Legal (contract Q&A), Product (documentation search), and every department with a knowledge base. Each team needs an assistant that understands their specific domain, using their specific documents.

### The Value Proposition
Shift resolution as close to the user as possible by intercepting Level 0/1 issues before they reach the queue, while simultaneously eliminating the documentation burden on agents. The platform is domain-agnostic — any team can adopt it, configure their LLM, upload their documents, and have an intelligent assistant serving their users in minutes.

## 3. The "What" (Core Product Features)

* **Feature 1: Multi-Tenant Workspaces:** Isolated workspaces with their own users, knowledge base, LLM configuration, and taxonomy. Workspace switching via UI dropdown.
* **Feature 2: Document Ingestion:** Upload markdown, plain text, PDF, HTML, XML, and Word documents via drag-and-drop. Async ETL pipeline extracts, chunks, and embeds content.
* **Feature 3: BYO LLM:** Each workspace configures its own OpenAI-compatible endpoint (any provider, any model). API keys encrypted at rest.
* **Feature 4: The Intelligent Assistant:** A conversational interface where users describe their issue. Hybrid search (FTS + vector + RRF) across articles and document chunks provides contextual answers.
* **Feature 5: Domain-Agnostic Taxonomy:** Workspaces define category/subcategory taxonomies and customize system prompts with template variables.
* **Feature 6: Seamless Contextual Ticketing:** If the automated guide fails, escalate to a human agent with full AI chat context preserved.
* **Feature 7: The Automated Knowledge Loop (KCS):** When an agent resolves a novel issue and flags it as a "Knowledge Gap," the system's AI drafts a new help article based on the ticket history.

## 4. The "How" (User Journeys)

### The End-User Journey (Knowledge Seeker)
1. User logs into the platform and selects or lands in their workspace.
2. User describes their issue to the AI assistant.
3. The system searches the workspace's knowledge base (articles + documents) and provides a step-by-step guide.
4. The system asks: *"Did this solve your problem?"*
5. If "Yes," the interaction ends (Ticket Deflected).
6. If "No," a ticket form appears, pre-filled with context. User hits "Submit."

### The Agent Journey (The Resolver)
1. Agent opens dashboard and selects the new ticket.
2. Reads the user's issue alongside the full "Shift-Left Context" (AI chat transcript).
3. Agent fixes the issue and types final steps into "Resolution Notes."
4. Agent checks the "Flag as Knowledge Gap" box and closes the ticket.

### The Workspace Admin Journey
1. Admin manages workspace settings: name, description, icon, members, LLM config.
2. Admin invites members with role selection (admin / member / read-only).
3. Admin configures the AI assistant's system prompt with domain-specific instructions.
4. Admin defines the workspace's knowledge taxonomy (categories and subcategories).
5. Admin manages uploaded documents and reviews KCS drafts.

## 5. The End Goal (Success Metrics)
Demonstrate a functional **Knowledge-Centered Service (KCS)** environment to recruiters. Success is defined by:
* **Ticket Deflection:** Intercepting simple queries before they reach the queue.
* **Reduced MTTR:** Providing agents with immediate context.
* **Organic Documentation Growth:** Eliminating the burden of writing from scratch.
* **Domain Agnosticism:** The platform is equally useful for IT, HR, Legal, and any other domain.
