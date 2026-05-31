# Shift-Left Knowledge Hub - Product Requirements Document (PRD)

## 1. The Overview
The "Shift-Left Knowledge Hub" is a modern IT Service Management (ITSM) web application designed to rethink how corporate helpdesks operate. Instead of functioning as a passive system where users simply submit tickets and wait, this platform proactively intercepts user issues. It provides an intelligent, automated assistant that guides users through troubleshooting based on approved company documentation. If the issue requires human intervention, it ensures the IT agent receives a ticket packed with context, and eventually uses the agent's fix to automatically write new documentation, creating a continuous loop of improvement.

## 2. The "Why" (Problem & Value Proposition)

### The Core Problem
IT support teams spend an exorbitant amount of time responding to "Level Zero" and "Level One" tickets—repetitive, easily solvable issues like password resets, VPN disconnections, or printer mappings. While self-service knowledge bases exist, users rarely use them because they are hard to search and often outdated. Conversely, IT agents are too busy closing tickets to write new documentation, leading to a stagnant, unhelpful knowledge base.

### The Value Proposition
This product solves both sides of the equation by shifting the resolution process as close to the user as possible (the "Shift-Left" strategy). It empowers users to fix their own problems quickly via an intelligent assistant, dramatically reducing the ticket queue. Simultaneously, it eliminates the friction of documentation by having the system automatically draft new articles based on successful ticket resolutions.

## 3. The "What" (Core Product Features)

* **Feature 1: The Intelligent Self-Service Portal:** A conversational interface where users describe their IT issue. The system searches existing documentation and provides a step-by-step resolution guide.
* **Feature 2: Seamless Contextual Ticketing:** If the automated guide fails, users can escalate to a human agent. The system creates a ticket pre-filled with the user's issue and a log of everything the AI tried.
* **Feature 3: The IT Agent Dashboard:** A clean, prioritized workspace for IT staff to view incoming tickets, read contextual history, and communicate resolution steps.
* **Feature 4: The Automated Knowledge Loop (KCS):** When an agent resolves a novel issue and flags it as a "Knowledge Gap," the system's AI drafts a new help article based on the ticket history.
* **Feature 5: Knowledge Base Administration:** A review queue where IT administrators can review, approve, and publish AI-drafted articles.

## 4. The "How" (User Journeys)

### The End-User Journey (The Requester)
1. User logs into the portal experiencing an issue.
2. The system provides a simplified, easy-to-read guide sourced from company files.
3. The system asks: *"Did this solve your problem?"*
4. If "Yes," the interaction ends (Ticket Deflected).
5. If "No," a ticket form appears, pre-filled with context. User hits "Submit."

### The IT Agent Journey (The Resolver)
1. Agent opens dashboard and selects the new ticket.
2. Reads the user's issue alongside the "Deflection Context".
3. Agent fixes the issue and types final steps into "Resolution Notes."
4. Agent checks the "Flag as Knowledge Gap" box and closes the ticket.

### The Admin Journey (The Knowledge Manager)
1. Admin checks the "Drafts" tab.
2. Reviews the AI-written article based on the recently closed ticket.
3. Makes formatting tweaks, assigns tags, and clicks "Publish."
4. Solution is instantly available to the intelligent assistant.

## 5. The End Goal (Success Metrics)
Demonstrate a functional **Knowledge-Centered Service (KCS)** environment to recruiters. Success is defined by:
* **Ticket Deflection:** Intercepting simple queries before they reach the queue.
* **Reduced MTTR:** Providing agents with immediate context.
* **Organic Documentation Growth:** Eliminating the burden of writing from scratch.