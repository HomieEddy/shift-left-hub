---
phase: 06-kcs-auto-drafting-admin-review
plan: 01
type: execute
subsystem: kcs
tags:
  - kcs
  - async
  - event-driven
  - article
  - agent-ticket
requires:
  - ticket domain entities (Ticket, TicketCategory, TicketUrgency)
  - Spring Boot async infrastructure
provides:
  - sourceTicketId on Article entity
  - TicketResolvedEvent record
  - AsyncConfig with @EnableAsync
  - AgentTicketService event publishing wiring
affects:
  - backend/src/main/java/com/shiftleft/hub/article/domain/Article.java
  - backend/src/main/java/com/shiftleft/hub/kcs/domain/TicketResolvedEvent.java (new)
  - backend/src/main/java/com/shiftleft/hub/config/AsyncConfig.java (new)
  - backend/src/main/java/com/shiftleft/hub/agent/service/AgentTicketService.java
tech-stack:
  added:
    - Spring @EnableAsync + ThreadPoolTaskExecutor
    - Spring ApplicationEventPublisher
  patterns:
    - Event-driven KCS pipeline via in-process events
    - Nullable UUID column with @Builder.Default exclusion (no default for nullable fields)
    - Try-catch around event publication to prevent transaction rollback
key-files:
  created:
    - backend/src/main/java/com/shiftleft/hub/kcs/domain/TicketResolvedEvent.java
    - backend/src/main/java/com/shiftleft/hub/config/AsyncConfig.java
  modified:
    - backend/src/main/java/com/shiftleft/hub/article/domain/Article.java
    - backend/src/main/java/com/shiftleft/hub/agent/service/AgentTicketService.java
decisions:
  - "sourceTicketId is a plain UUID column, not a @ManyToOne JPA relationship — keeps Article decoupled from Ticket module (per D-15)"
  - "TicketResolvedEvent carries full ticket data to avoid extra DB queries in the async listener (per D-03)"
  - "Event publication wrapped in try-catch per T-06-01 — KCS event failure must not roll back ticket resolution"
  - "No @Builder.Default on sourceTicketId — nullable UUID should default to null (Lombok requires explicit default value for @Builder.Default)"
metrics:
  duration: "12 minutes"
  completed_date: "2026-06-05"
  tasks: 3
  files_created: 2
  files_modified: 2
  commits: 3
---

# Phase 6 Plan 01: Backend KCS Foundation Summary

Backend KCS Foundation — Article entity extension with `sourceTicketId`, async event infrastructure (`@EnableAsync`), `TicketResolvedEvent` record creation, and event publishing wiring into `AgentTicketService.resolveTicket()`.

## Tasks Executed

### Task 1: Add sourceTicketId to Article + create TicketResolvedEvent
**Commit:** `7f7afd8`

- Added `private UUID sourceTicketId` column (nullable, `@Column(name = "source_ticket_id")`) to the Article entity, placed after `featuredImage` and before `status`
- Created `TicketResolvedEvent` Java record in the new `kcs/domain/` module directory with all 11 fields: ticketId, ticketNumber, issue, shiftLeftContext, category, urgency, resolutionNotes, userDisplayName, userEmail, agentDisplayName, resolvedAt
- Event record annotates each parameter with Javadoc describing its purpose
- **Deviation [Rule 1]**: Removed `@Builder.Default` from the `sourceTicketId` field — `@Builder.Default` requires an explicit default value expression, but the nullable UUID should default to `null`. Lombok's annotation processor failed to compile with `@Builder.Default` on an uninitialized UUID field.

### Task 2: Enable async execution infrastructure
**Commit:** `94d38ce`

- Created `AsyncConfig` with `@Configuration` and `@EnableAsync` annotations for application-wide async processing
- Defined `kcsTaskExecutor` bean with: core pool size 2, max pool size 4, queue capacity 10, `kcs-async-` thread name prefix, and graceful shutdown enabled

### Task 3: Wire event publishing into AgentTicketService.resolveTicket()
**Commit:** `3cb4661`

- Injected `ApplicationEventPublisher` via constructor (using existing `@RequiredArgsConstructor`)
- Added `TicketResolvedEvent` import
- After `ticketRepository.save(ticket)` and before the existing log line, added an event publication block:
  - Guarded by `if (isKnowledgeGap)` — no event when flag is false
  - Wrapped in try-catch per T-06-01 threat mitigation — failure logs but does not throw
  - Logs success or failure for observability

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Removed @Builder.Default from sourceTicketId field**
- **Found during:** Task 1 verification (mvn compile)
- **Issue:** Lombok's `@Builder.Default` requires an explicit default value (`= ...`). The nullable `sourceTicketId` UUID field should default to `null`. Without a default value expression, Lombok generates a reference to a non-existent `$default$sourceTicketId()` method.
- **Fix:** Removed `@Builder.Default` annotation from the field. Nullable fields default to `null` in Java without explicit initialization.
- **Files modified:** `backend/src/main/java/com/shiftleft/hub/article/domain/Article.java`
- **Commit:** `7f7afd8`

## Threat Model Compliance

| Threat ID | Disposition | Status |
|-----------|-------------|--------|
| T-06-01 | mitigate | ✅ Event publication wrapped in try-catch, logged at error level, ticket resolution unaffected |
| T-06-02 | accept | ✅ Single-tenant, authenticated users only — no rate limiting implemented |

## Verification Results

- ✅ `mvn compile` succeeds (verified after each task)
- ✅ Article.java has `private UUID sourceTicketId` field
- ✅ TicketResolvedEvent.java exists in `kcs/domain/` with all required fields
- ✅ AsyncConfig.java has `@EnableAsync` and `kcsTaskExecutor` bean
- ✅ ApplicationEventPublisher injected in AgentTicketService
- ✅ Event published after `save()` when `isKnowledgeGap=true` (guarded by `if`)
- ✅ Event publication failure handled gracefully (try-catch, non-blocking)
- ✅ No event published when `isKnowledgeGap=false` (noop path)

## Success Criteria

- [x] Article entity extended with sourceTicketId UUID column
- [x] TicketResolvedEvent record created in kcs/domain/ with full ticket data
- [x] @EnableAsync configured with dedicated kcsTaskExecutor
- [x] AgentTicketService wired to publish TicketResolvedEvent on knowledge-gap resolution
- [x] Backend compiles cleanly

## Self-Check: PASSED

All created files exist, all commits verified in git log, compilation succeeds.
