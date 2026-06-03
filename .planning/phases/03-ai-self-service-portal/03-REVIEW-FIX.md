---
phase: 03-ai-self-service-portal
fixed_at: 2026-06-03T22:00:00Z
review_path: .planning/phases/03-ai-self-service-portal/03-REVIEW.md
iteration: 1
findings_in_scope: 10
fixed: 10
skipped: 0
status: all_fixed
---

# Phase 3: Code Review Fix Report

**Fixed at:** 2026-06-03T22:00:00Z
**Source review:** .planning/phases/03-ai-self-service-portal/03-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 10 (3 critical + 7 warnings)
- Fixed: 10
- Skipped: 0

## Fixed Issues

### CR-01: Thread Leak in ChatController — ExecutorService Never Shut Down

**Files modified:** `backend/src/main/java/com/shiftleft/hub/ai/api/ChatController.java`, `backend/src/main/java/com/shiftleft/hub/config/SecurityConfig.java`
**Commit:** efaa7f1
**Applied fix:** Created a shared `ExecutorService` bean (`@Bean("chatExecutor")`) using `Executors.newVirtualThreadPerTaskExecutor()` (Java 21 virtual threads) in `SecurityConfig`. Modified `ChatController` to inject it via constructor with `@Qualifier("chatExecutor")`, replacing the per-request `Executors.newSingleThreadExecutor().submit(...)`.

### CR-02: Hardcoded Secrets in application.properties

**Files modified:** `backend/src/main/resources/application.properties`
**Commit:** c831778
**Applied fix:** Replaced hardcoded values with environment variable references with dev-safe defaults:
- `app.jwt.secret` → `${APP_JWT_SECRET:...}`
- `app.ai.encryption-key` → `${APP_AI_ENCRYPTION_KEY:...}`
- `spring.datasource.username` → `${DB_USERNAME:shiftleft}`
- `spring.datasource.password` → `${DB_PASSWORD:shiftleft}`

### CR-03: Swallowed IOException in ChatController Timeout Handler

**Files modified:** `backend/src/main/java/com/shiftleft/hub/ai/api/ChatController.java`
**Commit:** efaa7f1
**Applied fix:** Added `@Slf4j` annotation and replaced the empty catch block with `log.warn("Failed to send timeout error to client", e)`.

### WR-01: Swallowed IOException in AiChatService Token Stream

**Files modified:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java`
**Commit:** 9d38878
**Applied fix:** Added `log.debug("Client disconnected, aborting stream")` and `emitter.completeWithError(e)` in the `onNext` callback's IOException catch block.

### WR-02: Dynamic AI Config Never Actually Wired to ChatClient at Runtime

**Files modified:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java`, `backend/src/main/java/com/shiftleft/hub/ai/service/AiConfigService.java`
**Commit:** c01ab57
**Applied fix:** 
- **AiChatService:** Removed `ChatClient.Builder` dependency. Added `buildChatClient(AiConfig config)` method that constructs an appropriate `ChatModel` (`OpenAiChatModel` or `OllamaChatModel`) based on the stored config. Calls `aiConfigService.decrypt()` to decrypt the API key before passing to the OpenAI builder. `processChat()` now builds a per-request `ChatClient` from the database config.
- **AiConfigService:** Modified `testConnection()` to build a dynamic `ChatClient` from the request params instead of using the auto-configured builder. Removed unused `chatClientBuilder` field.

### WR-03: RRF Threshold Used Instead of Similarity Threshold

**Files modified:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java`
**Commit:** b5872b8
**Applied fix:** Moved the `similarityThreshold` to the `SearchRequest` in `vectorSearch()` via `.similarityThreshold(threshold)`. Removed the `.filter(e -> e.getValue() >= threshold)` from the RRF fusion stream, since RRF scores have different semantics from cosine similarity.

### WR-04: Unstable TrackBy Function in Chat Component

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** 3414a46
**Applied fix:** Changed `trackByFn` from `msg.content.length + msg.role` (unstable during streaming) to `return index` (stable since messages are only appended).

### WR-05: Unused Imports in AiChatService

**Files modified:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java`
**Commit:** ad91bbe
**Applied fix:** Removed three unused imports: `java.time.LocalDateTime`, `java.util.ArrayList`, `java.util.Comparator`.

### WR-06: Missing Input Validation on AiConfigRequest

**Files modified:** `backend/src/main/java/com/shiftleft/hub/ai/api/dto/AiConfigRequest.java`
**Commit:** b4386fc
**Applied fix:** Added `@jakarta.validation.constraints` annotations to the record fields:
- `@Pattern(regexp = "^(OLLAMA|OPENAI)$")` on `llmProvider`
- `@Pattern(regexp = "^https?://.*")` on `ollamaEndpointUrl`
- `@NotBlank` on `chatModelName`
- `@Min(0) @Max(1)` on `similarityThreshold`

Note: `@Valid` was NOT added to the controller methods to preserve partial-update behavior (null fields = skip update). Validation groups would be needed for that.

### WR-07: SSE Parser May Fail on Multi-Line Data (LLM Output with Newlines)

**Files modified:** `frontend/src/app/features/chat/chat.service.ts`
**Commit:** ea174f8
**Applied fix:** Rewrote the SSE parsing loop to accumulate multi-line data fields:
- Added `currentData` accumulator variable
- Lines starting with `data: {` start a new event; subsequent `data:` lines without a leading `{` are treated as continuations (per SSE spec)
- Empty lines signal end of an SSE event and trigger parsing
- Remaining data at end-of-stream is parsed on stream completion
- Extracted `tryParseAndEmit()` helper for cleaner logic

---

_Fixed: 2026-06-03T22:00:00Z_
_Fixer: gsd-code-fixer_
_Iteration: 1_
