---
phase: 06-kcs-auto-drafting-admin-review
plan: 02
type: execute
subsystem: kcs
tags:
  - kcs
  - ai-drafting
  - admin-api
  - event-driven
  - dedup
requires:
  - 06-01: Article entity sourceTicketId, TicketResolvedEvent, AsyncConfig, AgentTicketService event wiring
provides:
  - KcsDraftingService core drafting orchestration (AI synthesis + dedup + article creation)
  - KcsEventListener async event listener with 3x retry + exponential backoff
  - AdminKcsController REST API (list/detail/approve/reject/pending-count)
  - KcsDraftResponse DTO with source ticket number and similarity warnings
  - KcsDraftingException for non-retryable drafting errors
  - ArticleRepository KCS query methods (bySourceTicketId, countByStatus)
  - TagRepository.findByNameEnIn for suggested tag resolution
affects:
  - 06-03: Frontend admin KCS draft queue UI will consume these APIs
tech-stack:
  added:
    - Spring @TransactionalEventListener for post-commit event processing
    - Exponential backoff retry pattern for LLM failures
    - FTS fast-path + pgvector semantic dedup pipeline
  patterns:
    - AI article drafting with structured LLM prompt and response parsing
    - Duplicate detection via FTS keyword filter → pgvector semantic check
    - Async event listener with retryable vs non-retryable error distinction
    - Admin API endpoint pattern for draft queue lifecycle
key-files:
  created:
    - backend/src/main/java/com/shiftleft/hub/kcs/domain/KcsDraftingException.java
    - backend/src/main/java/com/shiftleft/hub/kcs/service/KcsDraftingService.java
    - backend/src/main/java/com/shiftleft/hub/kcs/service/KcsEventListener.java
    - backend/src/main/java/com/shiftleft/hub/kcs/api/dto/KcsDraftResponse.java
    - backend/src/main/java/com/shiftleft/hub/kcs/api/AdminKcsController.java
  modified:
    - backend/src/main/java/com/shiftleft/hub/article/domain/ArticleRepository.java
    - backend/src/main/java/com/shiftleft/hub/tag/domain/TagRepository.java
    - backend/src/main/java/com/shiftleft/hub/ai/service/AiConfigService.java
    - backend/src/main/java/com/shiftleft/hub/common/config/GlobalExceptionHandler.java
requirements-completed: [KCS-01, KCS-02, KCS-03, KCS-04, ADM-01, ADM-02]
metrics:
  duration: "12 minutes"
  completed_date: "2026-06-05"
  tasks: 3
  files_created: 5
  files_modified: 4
  commits: 3
---

# Phase 6 Plan 02: KCS Drafting Engine + Admin API Summary

**AI synthesis, pgvector dedup, async event listener with retry, and admin draft queue REST API — 5 new files, 4 modified, backend compiles cleanly**

## Performance

- **Duration:** 12 minutes
- **Started:** 2026-06-05T10:52:26Z
- **Completed:** 2026-06-05T11:04:30Z
- **Tasks:** 3
- **Files created:** 5
- **Files modified:** 4

## Accomplishments

- **KcsDraftingService** — Core orchestration: builds structured LLM prompt from ticket timeline, calls LLM (OpenAI/Ollama), parses bilingual response, checks duplicates via FTS fast-path + pgvector semantic search (>0.85 threshold), creates DRAFT article with sourceTicketId link, resolves suggested tags by name_en
- **KcsEventListener** — Async event handler with `@Async("kcsTaskExecutor")` + `@TransactionalEventListener(AFTER_COMMIT)`: retries LLM failures up to 3 times with exponential backoff (1s/2s/4s), adds auto-generated work note on success, distinguishes retryable (timeout/connection/5xx/rate-limit) vs non-retryable (DB/state) errors
- **AdminKcsController** — 5 REST endpoints at `/api/admin/kcs/drafts`: list (paginated), detail by ID, approve (→ PUBLISHED), reject (→ ARCHIVED), pending-count (for nav badge)
- **KcsDraftResponse** — Record DTO with sourceTicketId, sourceTicketNumber, similarityWarnings, tags, and standard article fields
- **Repository extensions** — ArticleRepository with 3 KCS query methods (findBySourceTicketIdIsNotNullOrderByCreatedAtDesc, findBySourceTicketId, countBySourceTicketIdIsNotNullAndStatus) + TagRepository.findByNameEnIn
- **Exception handling** — KcsDraftingException registered in GlobalExceptionHandler
- **Deviation fix** — Made `AiConfigService.decrypt()` public for cross-package access

## Task Commits

Each task was committed atomically:

1. **Task 1: KcsDraftingException + KcsDraftingService + repository extensions** — `1c96bed` (feat)
2. **Task 2: KcsEventListener with retry logic and work note** — `89ad3c5` (feat)
3. **Task 3: AdminKcsController + KcsDraftResponse + GlobalExceptionHandler** — `1e016e0` (feat)

## Files Created/Modified

### Created
- `backend/src/main/java/com/shiftleft/hub/kcs/domain/KcsDraftingException.java` — Custom exception for non-retryable KCS drafting errors
- `backend/src/main/java/com/shiftleft/hub/kcs/service/KcsDraftingService.java` — Core drafting orchestration (AI synthesis, dedup, article creation)
- `backend/src/main/java/com/shiftleft/hub/kcs/service/KcsEventListener.java` — Async event listener with 3x retry + work note generation
- `backend/src/main/java/com/shiftleft/hub/kcs/api/dto/KcsDraftResponse.java` — DTO for draft queue items with source ticket number and similarity warnings
- `backend/src/main/java/com/shiftleft/hub/kcs/api/AdminKcsController.java` — REST controller for admin draft queue lifecycle

### Modified
- `backend/src/main/java/com/shiftleft/hub/article/domain/ArticleRepository.java` — Added 3 KCS query methods
- `backend/src/main/java/com/shiftleft/hub/tag/domain/TagRepository.java` — Added findByNameEnIn for tag resolution
- `backend/src/main/java/com/shiftleft/hub/ai/service/AiConfigService.java` — Made decrypt() public (cross-package access)
- `backend/src/main/java/com/shiftleft/hub/common/config/GlobalExceptionHandler.java` — Added KcsDraftingException handler

## Decisions Made

- Followed the existing `AiChatService.buildChatClient()` pattern for LLM interaction — duplicated the factory method in `KcsDraftingService` rather than injecting `AiChatService` (keeps drafting service self-contained, avoids coupling to chat concerns)
- Work note added in `KcsEventListener` (not `AgentTicketService`) — cleaner separation of concerns: listener owns post-draft side effects
- FTS fast-path as a pre-filter before pgvector semantic search for dedup — avoids costly vector queries for obviously unique content
- System user (`system@shiftleft.local`) created inline with lazy initialization in the listener — no migration needed

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Made AiConfigService.decrypt() public for cross-package access**
- **Found during:** Task 1 compilation verification (`mvn compile`)
- **Issue:** `KcsDraftingService` (in `com.shiftleft.hub.kcs.service`) calls `AiConfigService.decrypt()` to decrypt the OpenAI API key. The `decrypt()` method had package-private visibility (`String decrypt(...)` instead of `public String decrypt(...)`), causing a compilation error.
- **Fix:** Changed `String decrypt(String ciphertext)` to `public String decrypt(String ciphertext)` in `AiConfigService.java`
- **Files modified:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiConfigService.java`
- **Verification:** `mvn compile` passes after the change
- **Committed in:** `1c96bed` (Task 1 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Necessary fix for cross-package method access. No scope creep — method was already used by `AiConfigService.testConnection()` and the AI chat, just needed visibility widened.

## Threat Model Compliance

| Threat ID | Category | Component | Disposition | Status |
|-----------|----------|-----------|-------------|--------|
| T-06-03 | S | KcsEventListener | mitigate | ✅ All LLM/drafting exceptions handled, retry on transient failures, fail gracefully on permanent errors. Ticket resolution never blocked. |
| T-06-04 | T | KcsDraftingService.llm | accept | ✅ AI content reviewed by admin before publication. Dedup check prevents duplicate publishing. Admin approval gate (publish/archive) in place. |
| T-06-05 | D | AdminKcsController | mitigate | ✅ ADMIN role required via existing `/api/admin/**` security pattern. |
| T-06-06 | I | LLM request/response | accept | ✅ LLM traffic local (Ollama) or HTTPS (OpenAI). No PII beyond existing ticket content. |
| T-06-07 | E | LLM outage | mitigate | ✅ Retry with exponential backoff (3 attempts). After exhaustion: log error, continue without draft. Ticket resolution never blocked. |

## Verification Results

- ✅ `mvn compile` succeeds (verified after each task)
- ✅ KcsDraftingException created with two constructors
- ✅ KcsDraftingService with full AI synthesis pipeline (prompt → LLM → parse → dedup → create)
- ✅ Duplicate detection via FTS fast-path + pgvector semantic search (>0.85 threshold)
- ✅ KcsEventListener annotated with `@Async("kcsTaskExecutor")` and `@TransactionalEventListener(AFTER_COMMIT)`
- ✅ Retry logic: 3 attempts with exponential backoff (1s/2s/4s)
- ✅ Error classification: retryable (timeout/connection/5xx/rate-limit) vs non-retryable (KcsDraftingException)
- ✅ Work note auto-generation on successful draft creation
- ✅ System user creation via `getOrCreateSystemUser()` (system@shiftleft.local)
- ✅ AdminKcsController with 5 endpoints: `GET /`, `GET /{id}`, `PUT /{id}/approve`, `PUT /{id}/reject`, `GET /pending-count`
- ✅ KcsDraftResponse DTO with sourceTicketNumber and similarityWarnings fields
- ✅ KcsDraftingException handler registered in GlobalExceptionHandler (500 INTERNAL_SERVER_ERROR)
- ✅ ArticleRepository: findBySourceTicketIdIsNotNullOrderByCreatedAtDesc, findBySourceTicketId, countBySourceTicketIdIsNotNullAndStatus
- ✅ TagRepository: findByNameEnIn(Collection<String>)
- ✅ No unintended file deletions
- ✅ No untracked files left behind

## Next Phase Readiness

- Backend KCS drafting engine complete — ready for **06-03 frontend admin KCS draft queue UI**
- All 5 API endpoints available at `/api/admin/kcs/drafts` for the frontend to consume
- `GET /api/admin/kcs/drafts/pending-count` returns `{ "pendingCount": <number> }` for the nav badge
- `PUT /{id}/approve` publishes immediately via existing `ArticleService.publishArticle()` (triggers embedding generation)
- `PUT /{id}/reject` archives via existing `ArticleService.archiveArticle()`

## Self-Check: PASSED

All created files exist, all commits verified in git log, compilation succeeds.

---
*Phase: 06-kcs-auto-drafting-admin-review*
*Completed: 2026-06-05*
