# Requirements: Shift-Left Knowledge Hub

**Defined:** 2026-05-31
**Core Value:** Shift resolution as close to the user as possible by intercepting Level 0/1 issues before they reach the queue, while simultaneously eliminating the documentation burden on IT agents.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Authentication

- [x] **AUTH-01**: User can sign up with email and password — *Plan 01-02*
- [x] **AUTH-02**: User can log in with credentials and receive a JWT session — *Plan 01-02*
- [x] **AUTH-03**: User session persists across browser refresh via refresh token rotation — *Plan 01-02*
- [x] **AUTH-04**: User is assigned a role (ROLE_USER or ROLE_ADMIN) — *Plan 01-02*

### Knowledge Base

- [x] **KB-01**: Admin can create, edit, and archive knowledge base articles in markdown — *Plan 02-01 (schema)*
- [x] **KB-02**: User can browse published articles with title and tag listing — *Plan 02-04*
- [x] **KB-03**: User can search articles using full-text search with result snippets — *Plan 02-04*
- [x] **KB-04**: Articles support tags for categorization (Tag entity with M2M) — *Plan 02-01*
- [x] **KB-05**: Articles are bilingual — separate content per language (EN/FR columns) — *Plan 02-01*

### AI Self-Service Portal

- [x] **AI-01**: User can describe an IT issue in a conversational chat interface
- [x] **AI-02**: System searches knowledge base using hybrid search (FTS + vector) and returns step-by-step resolution guides
- [x] **AI-03**: AI responses stream in real-time via SSE with a typing indicator
- [x] **AI-04**: System asks "Did this solve your problem?" after presenting a guide
- [x] **AI-05**: If no relevant article found, system returns a helpful fallback message and offers escalation
- [x] **AI-06**: System enforces similarity threshold (>0.65) to prevent hallucinated responses

### Escalation & Ticketing

- [ ] **TKT-01**: User can escalate an unresolved issue to a human agent
- [ ] **TKT-02**: Escalation creates a ticket pre-filled with the user's issue and full AI chat transcript (shift-left context)
- [ ] **TKT-03**: User selects category (NETWORK, HARDWARE, SOFTWARE, ACCESS, PERIPHERALS) and urgency (LOW, MEDIUM, HIGH)
- [ ] **TKT-04**: Ticket status tracks through NEW → IN_PROGRESS → RESOLVED

### Agent Dashboard

- [x] **AGT-01**: IT agent can view a prioritized list of all tickets
- [x] **AGT-02**: Agent can filter tickets by status, category, and urgency
- [x] **AGT-03**: Agent can view full ticket detail including shift-left deflection context
- [x] **AGT-04**: Agent can add resolution notes and mark ticket as resolved
- [x] **AGT-05**: Agent can flag a resolved ticket as a "Knowledge Gap" for KCS drafting

### KCS Auto-Drafting

- [ ] **KCS-01**: When an agent resolves a ticket flagged as Knowledge Gap, the system automatically drafts a KB article
- [ ] **KCS-02**: The AI synthesizes article content from the ticket timeline and resolution notes
- [ ] **KCS-03**: Drafted articles are saved with status DRAFT and linked to the source ticket
- [ ] **KCS-04**: The system checks for duplicate articles before creating a new draft

### Admin Console

- [ ] **ADM-01**: Admin can view a queue of AI-drafted articles pending review
- [ ] **ADM-02**: Admin can edit, approve (→ PUBLISHED), or reject (→ ARCHIVED) draft articles
- [x] **ADM-03**: Admin can manage users (view, assign roles)
- [x] **ADM-04**: Admin can manage article tags — *Plan 02-03*

### Infrastructure

- [x] **INF-01**: Local development environment runs via Docker Compose (PostgreSQL 16 + pgvector)
- [x] **INF-02**: Backend runs on Spring Boot 3.x with Java 21 — *Plan 01-02*
- [x] **INF-03**: Frontend serves as a standalone Angular SPA
- [x] **INF-04**: The AI module works with a local LLM fallback (Ollama) for API-key-free demo

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap.

### Notifications

- **NOTF-01**: User receives email when ticket status changes
- **NOTF-02**: Agent receives notification when new ticket is created
- **NOTF-03**: Admin receives notification when new AI draft is ready for review

### Quality & Analytics

- **QAL-01**: User can rate article helpfulness (thumbs up/down)
- **QAL-02**: Admin can view dashboard with deflection rate and MTTR metrics
- **QAL-03**: System tracks which articles resolve the most tickets

### Advanced Features

- **ADV-01**: Article versioning with diff view
- **ADV-02**: Bulk article import/export
- **ADV-03**: OAuth/SSO login (Google, GitHub)

## Out of Scope

| Feature | Reason |
|---------|--------|
| Email notifications | Not core to KCS loop, defers to v2 |
| SLA management | Enterprise feature, no portfolio demo value |
| Real-time agent chat | Destroys async workflow benefits, requires staffing |
| Mobile native app | Responsive SPA is sufficient |
| Elasticsearch | Overkill — PostgreSQL FTS + pgvector handles all search needs |
| Kafka/RabbitMQ | Unnecessary infrastructure for modular monolith |
| SSO/OAuth | Setup complexity not justified for portfolio demo |
| Multi-tenancy | Single-tenant portfolio app |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 1 (Plan 01-02) | Complete |
| AUTH-02 | Phase 1 (Plan 01-02) | Complete |
| AUTH-03 | Phase 1 (Plan 01-02) | Complete |
| AUTH-04 | Phase 1 (Plan 01-02) | Complete |
| KB-01 | Phase 2 (Plan 02-01) | Complete |
| KB-02 | Phase 2 (Plan 02-04) | Complete |
| KB-03 | Phase 2 (Plan 02-04) | Complete |
| KB-04 | Phase 2 (Plan 02-01) | Complete |
| KB-05 | Phase 2 (Plan 02-01/02-04) | Complete |
| AI-01 | Phase 3 | Complete |
| AI-02 | Phase 3 | Complete |
| AI-03 | Phase 3 | Complete |
| AI-04 | Phase 3 | Complete |
| AI-05 | Phase 3 | Complete |
| AI-06 | Phase 3 | Complete |
| TKT-01 | Phase 4 | Pending |
| TKT-02 | Phase 4 | Pending |
| TKT-03 | Phase 4 | Pending |
| TKT-04 | Phase 4 | Pending |
| AGT-01 | Phase 5 | Pending |
| AGT-02 | Phase 5 | Pending |
| AGT-03 | Phase 5 | Pending |
| AGT-04 | Phase 5 | Pending |
| AGT-05 | Phase 5 | Pending |
| KCS-01 | Phase 6 | Pending |
| KCS-02 | Phase 6 | Pending |
| KCS-03 | Phase 6 | Pending |
| KCS-04 | Phase 6 | Pending |
| ADM-01 | Phase 6 | Pending |
| ADM-02 | Phase 6 | Pending |
| ADM-03 | Phase 1 | Complete |
| ADM-04 | Phase 2 (Plan 02-03) | Complete |
| INF-01 | Phase 1 | Complete |
| INF-02 | Phase 1 | Complete |
| INF-03 | Phase 1 | Complete |
| INF-04 | Phase 3 | Complete |

**Coverage:**
- v1 requirements: 36 total
- Mapped to phases: 36
- Unmapped: 0 ✓

| Phase | Requirement Count | Requirements |
|-------|-------------------|--------------|
| Phase 1: Foundation | 8 | AUTH-01, AUTH-02, AUTH-03, AUTH-04, ADM-03, INF-01, INF-02, INF-03 |
| Phase 2: Knowledge Base | 6 | KB-01, KB-02, KB-03, KB-04, KB-05, ADM-04 |
| Phase 3: AI Self-Service Portal | 7 | AI-01, AI-02, AI-03, AI-04, AI-05, AI-06, INF-04 |
| Phase 4: Escalation & Ticketing | 4 | TKT-01, TKT-02, TKT-03, TKT-04 |
| Phase 5: Agent Dashboard | 5 | AGT-01, AGT-02, AGT-03, AGT-04, AGT-05 |
| Phase 6: KCS Auto-Drafting & Admin Review | 6 | KCS-01, KCS-02, KCS-03, KCS-04, ADM-01, ADM-02 |
| Phase 7: Quality, Polish & DevOps | 0 | (polish phase — no v1 requirements) |

---
*Requirements defined: 2026-05-31*
*Last updated: 2026-06-01 after Phase 2 execution*
