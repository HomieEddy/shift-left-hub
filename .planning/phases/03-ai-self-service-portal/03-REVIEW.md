---
phase: 03-ai-self-service-portal
reviewed: 2026-06-03T21:00:00Z
depth: standard
files_reviewed: 27
files_reviewed_list:
  - backend/pom.xml
  - backend/src/main/java/com/shiftleft/hub/ai/api/AiConfigController.java
  - backend/src/main/java/com/shiftleft/hub/ai/api/ChatController.java
  - backend/src/main/java/com/shiftleft/hub/ai/api/dto/AiConfigRequest.java
  - backend/src/main/java/com/shiftleft/hub/ai/api/dto/AiConfigResponse.java
  - backend/src/main/java/com/shiftleft/hub/ai/api/dto/ChatRequest.java
  - backend/src/main/java/com/shiftleft/hub/ai/api/dto/StreamEvent.java
  - backend/src/main/java/com/shiftleft/hub/ai/api/dto/TestConnectionResult.java
  - backend/src/main/java/com/shiftleft/hub/ai/domain/AiConfig.java
  - backend/src/main/java/com/shiftleft/hub/ai/domain/AiConfigRepository.java
  - backend/src/main/java/com/shiftleft/hub/ai/domain/package-info.java
  - backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java
  - backend/src/main/java/com/shiftleft/hub/ai/service/AiConfigService.java
  - backend/src/main/java/com/shiftleft/hub/ai/service/EmbeddingService.java
  - backend/src/main/java/com/shiftleft/hub/article/service/ArticleService.java
  - backend/src/main/java/com/shiftleft/hub/common/config/DataSeeder.java
  - backend/src/main/java/com/shiftleft/hub/config/SecurityConfig.java
  - backend/src/main/resources/application.properties
  - frontend/src/app/app.html
  - frontend/src/app/app.routes.ts
  - frontend/src/app/features/admin/llm-settings/llm-settings.component.ts
  - frontend/src/app/features/admin/llm-settings/llm-settings.component.html
  - frontend/src/app/features/admin/llm-settings/llm-settings.service.ts
  - frontend/src/app/features/chat/chat.component.ts
  - frontend/src/app/features/chat/chat.component.html
  - frontend/src/app/features/chat/chat.service.ts
  - frontend/src/locale/messages.xlf
  - frontend/src/locale/messages.fr.xlf
findings:
  critical: 3
  warning: 7
  info: 5
  total: 15
status: issues_found
---

# Phase 3: Code Review Report — AI Self-Service Portal

**Reviewed:** 2026-06-03T21:00:00Z
**Depth:** standard
**Files Reviewed:** 27
**Status:** issues_found

## Summary

Reviewed 27 files from the Phase 3 AI Self-Service Portal implementation (backend Spring Boot + frontend Angular). The implementation covers the AI configuration CRUD, streaming chat via SSE, hybrid search (FTS + pgvector + RRF), embedding generation, and the admin LLM settings UI.

**Overall assessment:** Solid structural foundation with 3 critical issues, 7 warnings, and 5 info items. The most severe issues are a thread leak in `ChatController` (every chat request leaks an ExecutorService thread), hardcoded secrets in `application.properties`, and a significant architectural gap where the dynamic AI configuration stored in the database is never actually wired into the Spring AI `ChatClient` at runtime.

---

## Critical Issues

### CR-01: Thread Leak in ChatController — ExecutorService Never Shut Down

**File:** `backend/src/main/java/com/shiftleft/hub/ai/api/ChatController.java:29`

**Issue:** `Executors.newSingleThreadExecutor().submit(...)` creates a new thread pool on every chat request but never calls `shutdown()` or `shutdownNow()`. Under sustained load, each SSE chat request spawns a thread that remains alive indefinitely (default keep-alive for cached thread pools), leading to eventual thread exhaustion and `OutOfMemoryError: unable to create new native thread`.

**Fix:** Inject a shared `ExecutorService` bean with proper configuration, or use Spring's `@Async` with a configured task executor. Always shut down in a `finally` block or rely on bean lifecycle management.

```java
// Option A: Shared executor bean in config
@Bean("chatExecutor")
public ExecutorService chatExecutor() {
    return Executors.newVirtualThreadPerTaskExecutor(); // Java 21 virtual threads
}

// Option B: Inject in ChatController
private final ExecutorService chatExecutor;

public ChatController(AiChatService aiChatService, @Qualifier("chatExecutor") ExecutorService chatExecutor) {
    this.aiChatService = aiChatService;
    this.chatExecutor = chatExecutor;
}

// Then use:
chatExecutor.submit(() -> { ... });
```

---

### CR-02: Hardcoded Secrets in application.properties

**File:** `backend/src/main/resources/application.properties:16-17, 21`

**Issue:** Three production secrets are hardcoded in the committed configuration file:

1. **JWT Signing Secret** (line 16): `app.jwt.secret=shiftleft-jwt-secret-key-change-in-production-minimum-256-bits-long`
2. **AI Encryption Key** (line 21): `app.ai.encryption-key=shiftleft-ai-key-32-chars-base64-padded`
3. **Database Credentials** (lines 6-7): `spring.datasource.username=shiftleft`, `spring.datasource.password=shiftleft`

All three are committed to version control. The "change in production" comment is insufficient — if a developer deploys without overriding via environment variables, the default values will be used and are visible in the repository.

**Fix:** Externalize all three to environment variables with proper defaults only for development:

```properties
app.jwt.secret=${APP_JWT_SECRET}
app.ai.encryption-key=${APP_AI_ENCRYPTION_KEY:default-dev-key-do-not-use-in-prod}
spring.datasource.username=${DB_USERNAME:shiftleft}
spring.datasource.password=${DB_PASSWORD:shiftleft}
```

Add a `.env.example` file documenting required environment variables (excluded from VCS via `.gitignore`).

---

### CR-03: Swallowed IOException in ChatController Timeout Handler

**File:** `backend/src/main/java/com/shiftleft/hub/ai/api/ChatController.java:49`

**Issue:** The timeout callback silently swallows any `IOException` from `emitter.send()`:

```java
emitter.onTimeout(() -> {
    try {
        emitter.send(SseEmitter.event()
            .name("message")
            .data(new StreamEvent("error", "Request timed out after 30 seconds", null)));
    } catch (IOException e) {
        // silently ignored
    }
    emitter.complete();
});
```

If the emitter has already been completed or the client has disconnected, the error is lost. At minimum, the exception should be logged. The same silent-swallow pattern appears in `AiChatService.java:100` (WR-01).

**Fix:** Log the exception:

```java
} catch (IOException e) {
    log.warn("Failed to send timeout error to client", e);
}
```

---

## Warnings

### WR-01: Swallowed IOException in AiChatService Token Stream

**File:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java:96-100`

**Issue:** The `onNext` callback in the Reactor stream silently swallows `IOException` when sending token chunks to the SSE emitter:

```java
.subscribe(
    chunk -> {
        if (chunk != null && !chunk.isEmpty()) {
            fullResponse.updateAndGet(s -> s + chunk);
            try {
                emitter.send(...);
            } catch (IOException e) {
                // silently swallowed
            }
        }
    }
);
```

When the client disconnects, the SSE send fails but processing continues — tokens are still accumulated in `fullResponse`, and the stream continues until the LLM finishes. This wastes LLM resources and delays thread cleanup.

**Fix:** On IOException (client disconnect), abort the subscription stream and complete the emitter:

```java
} catch (IOException e) {
    log.debug("Client disconnected, aborting stream");
    // The subscription doesn't have a direct abort handle here,
    // but we can complete the emitter with error
    emitter.completeWithError(e);
}
```

---

### WR-02: Dynamic AI Config Never Actually Wired to ChatClient at Runtime

**Files:**
- `backend/src/main/java/com/shiftleft/hub/ai/service/AiConfigService.java:87-92`
- `backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java:63-64`

**Issue:** The dynamic AI configuration stored in the database (provider, model name, endpoint URL, API key) is **never applied to the Spring AI `ChatClient.Builder`**. Both the chat service and connection test use the auto-configured `ChatClient.Builder`, which reads from `application.properties` at application startup.

- `AiChatService.processChat()` at line 64: `chatClientBuilder.build()` — uses startup-time config only
- `AiConfigService.testConnection()` at line 87: `chatClientBuilder.build()` — tests against startup config, NOT the one shown in the UI

This means:
1. Changing the provider from Ollama to OpenAI via the Settings UI has **zero effect** on actual chat requests
2. The "Test Connection" button always tests against the startup-configured provider, not the one the user selected
3. The entire dynamic AI config persistence layer (`AiConfig`, `AiConfigRepository`, `AiConfigService.updateConfig()`) has no runtime effect

**Fix:** The `ChatClient.Builder` needs to be configured dynamically at request time using the stored configuration:

```java
public ChatClient buildChatClient(AiConfig config) {
    ChatClient.Builder builder = ChatClient.builder(chatModel(config));
    // Alternatively, use Spring AI's ChatClient.Builder with custom config
    return builder.build();
}

private ChatModel chatModel(AiConfig config) {
    if ("OPENAI".equals(config.getLlmProvider())) {
        String decryptedKey = decrypt(config.getOpenaiApiKey());
        return OpenAiChatModel.builder()
            .apiKey(decryptedKey)
            .model(config.getChatModelName())
            .build();
    }
    // Ollama default
    return OllamaChatModel.builder()
        .baseUrl(config.getOllamaEndpointUrl())
        .model(config.getChatModelName())
        .build();
}
```

**Note:** This requires the `decrypt()` method (already written in `AiConfigService` but never called — see IN-04) to actually be invoked. Current code encrypts the API key on save but never decrypts it on read.

---

### WR-03: RRF Threshold Used Instead of Similarity Threshold (Semantic Mismatch)

**File:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java:137-138`

**Issue:** The `similarityThreshold` config field (documented as vector similarity threshold, default 0.65) is applied as an **RRF score** filter, not a vector similarity filter:

```java
return rrfScores.entrySet().stream()
    .filter(e -> e.getValue() >= threshold)  // Applies to RRF score, not vector similarity
```

RRF scores have different semantics from cosine similarity. An RRF score of 0.65 corresponds to a different relevance level than a cosine similarity of 0.65. The config documentation in AGENTS.md says "Always set similarityThreshold > 0.65 in Spring AI RAG" — this threshold should be applied at the vector search level (as a `similarityThreshold` on `SearchRequest`), not to the fused RRF score.

**Fix:** Apply the similarity threshold at the vector search level, and use a separate mechanism (or no threshold) for RRF fusion:

```java
// Apply threshold to vector search
SearchRequest.builder()
    .query(query)
    .topK(TOP_K)
    .similarityThreshold(threshold)  // actual vector similarity threshold
    .build();

// Remove the threshold filter from RRF fusion (or use a different, documented value)
```

---

### WR-04: Unstable TrackBy Function in Chat Component

**File:** `frontend/src/app/features/chat/chat.component.ts:127-129`

**Issue:** The `trackByFn` uses `msg.content.length + msg.role` as a key. Since message content changes during token streaming, the key changes on every received token, causing Angular to destroy and recreate DOM elements instead of reusing them. This defeats the purpose of trackBy and causes unnecessary DOM churn:

```typescript
trackByFn(_index: number, msg: ChatMessage) {
    return msg.content.length + msg.role;
}
```

**Fix:** Use a stable unique identifier. Add an `id` field to `ChatMessage` or use the message index:

```typescript
// Option A: Use index (simplest, since messages are only appended)
trackByFn(index: number, _msg: ChatMessage) {
    return index;
}

// Option B: Add stable IDs to ChatMessage model
// ChatMessage: { id: number; role: 'user' | 'assistant'; content: string }
// Then: trackByFn(_index: number, msg: ChatMessage) { return msg.id; }
```

---

### WR-05: Unused Imports in AiChatService

**File:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java:19-21`

**Issue:** Three imports are unused in `AiChatService`:

- `java.time.LocalDateTime` (line 19) — never referenced in the file
- `java.util.ArrayList` (line 20) — no ArrayList used; all collections use HashMap, List.of(), or stream().toList()
- `java.util.Comparator` (line 21) — `Map.Entry.comparingByValue()` is a static method, not a direct Comparator reference

**Fix:** Remove unused imports:

```java
// Remove these three lines
```

---

### WR-06: Missing Input Validation on AiConfigRequest

**File:** `backend/src/main/java/com/shiftleft/hub/ai/api/dto/AiConfigRequest.java:3-10`

**Issue:** The `AiConfigRequest` record has no validation annotations. Fields like `llmProvider`, `ollamaEndpointUrl`, and `chatModelName` can contain empty strings, invalid URLs, or nonsense values. While partial updates are supported (null fields are skipped), empty strings are accepted and stored. This can lead to a broken system state (e.g., empty model name causes LLM calls to fail with confusing errors).

**Fix:** Add validation annotations:

```java
public record AiConfigRequest(
    @Pattern(regexp = "^(OLLAMA|OPENAI)$", message = "Provider must be OLLAMA or OPENAI")
    String llmProvider,

    @Pattern(regexp = "^https?://.*", message = "Must be a valid HTTP URL")
    String ollamaEndpointUrl,

    String openaiApiKey,

    @NotBlank(groups = WhenProviderSet.class)
    String chatModelName,

    String embeddingModelName,

    @Min(0) @Max(1)
    Double similarityThreshold
) {}
```

---

### WR-07: SSE Parser May Fail on Multi-Line Data (LLM Output with Newlines)

**File:** `frontend/src/app/features/chat/chat.service.ts:43-61`

**Issue:** The SSE parser reads line by line looking for the `data:` prefix. If the LLM response contains newlines (common in code blocks, lists, or multi-paragraph answers), Spring's `SseEmitter` will split the data across multiple `data:` lines per the SSE spec. The current parser treats each `data:` line as a separate JSON object, which will fail to parse for multi-line data:

```
event: message
data: {"type":"token","content":"Here is a list:\n- item 1\n- item 2",...}
```

Spring formats this as:
```
data: {"type":"token","content":"Here is a list:
data: - item 1
data: - item 2",...}
```

Each partial JSON will fail parsing at line 50, silently skip, and concatenated partial JSON is lost.

**Fix:** Accumulate multi-line data fields before parsing:

```typescript
let currentData = '';
for (const line of lines) {
    if (line.startsWith('data: ')) {
        const payload = line.slice(6);
        // Check if this is a continuation of previous data
        if (currentData && !payload.startsWith('{')) {
            currentData += '\n' + payload;
        } else {
            // Try parsing accumulated data first
            if (currentData) {
                try {
                    const event = JSON.parse(currentData);
                    subject.next(event);
                    // ... handle terminal events
                } catch { /* skip */ }
            }
            currentData = payload;
        }
    }
}
// Also parse remaining data after loop ends
```

---

## Info

### IN-01: SecureRandom.getInstanceStrong() May Block on Low-Entropy Systems

**File:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiConfigService.java:109`

**Issue:** `SecureRandom.getInstanceStrong()` uses a blocking entropy source (`/dev/random` on Linux), which can hang on headless servers or containers with low entropy. This affects the `encrypt()` method that generates IVs.

**Fix:** Use `new SecureRandom()` instead, which uses `/dev/urandom` and does not block:

```java
byte[] iv = new byte[12];
new SecureRandom().nextBytes(iv);  // non-blocking
```

---

### IN-02: SSE Timeout Not Coordinated with LLM Response Time

**File:** `backend/src/main/java/com/shiftleft/hub/ai/api/ChatController.java:27`

**Issue:** The SSE emitter timeout is fixed at 30 seconds (`new SseEmitter(30_000L)`), but there's no corresponding timeout configured on the ChatClient stream. If the LLM response takes longer than 30 seconds (e.g., large model, cold start, network latency), the SSE timeout fires mid-stream, sending a confusing timeout error while the LLM continues generating.

**Fix:** Either increase the timeout to account for LLM latency, or make it configurable. Consider using a longer timeout (e.g., 120 seconds) that can be adjusted through configuration:

```java
@Value("${app.ai.chat.timeout:120000}")
private long chatTimeout;
// ...
SseEmitter emitter = new SseEmitter(chatTimeout);
```

---

### IN-03: onProviderChange() Resets API Key but Doesn't Clear Stored Key on Save

**File:** `frontend/src/app/features/admin/llm-settings/llm-settings.component.ts:44-49, 59`

**Issue:** When the user switches from Ollama to OpenAI and back, `onProviderChange()` sets `this.openaiApiKey = ''`. When saving with Ollama selected, the `openaiApiKey` field sends as `undefined` (`this.openaiApiKey || undefined`), which the backend treats as "don't update." This means the previously stored OpenAI key remains encrypted in the database even after switching to Ollama. Not necessarily a bug, but creates orphaned secrets.

**Fix:** Consider either clearing the key when switching providers explicitly, or document the behavior as intended (key persists for easy switching back).

---

### IN-04: decrypt() Method Defined but Never Called

**File:** `backend/src/main/java/com/shiftleft/hub/ai/service/AiConfigService.java:122-139`

**Issue:** The `decrypt()` method exists but has no callers in the codebase. The API key is encrypted on save (line 65: `config.setOpenaiApiKey(encrypt(...))`), but there's no code path that reads and decrypts it for actual use. Combined with WR-02, the entire encryption/decryption cycle is dead code — the encrypted key is stored, never decrypted, and never used by any ChatClient.

**Fix:** Either:
1. Remove the encryption layer if the dynamic config isn't ready to be wired (YAGNI), or
2. Wire the decrypted key to a per-request ChatClient (see WR-02)

---

### IN-05: Templates Don't Use @angular/localize i18n Attributes Despite XLF Files

**Files:**
- `frontend/src/app/features/chat/chat.component.html`
- `frontend/src/app/features/admin/llm-settings/llm-settings.component.html`
- `frontend/src/app/app.html`
- `frontend/src/locale/messages.xlf`
- `frontend/src/locale/messages.fr.xlf`

**Issue:** The XLF translation files contain entries for all Phase 3 UI text (chat titles, placeholders, LLM settings labels, etc.), but the component templates use raw English text without `i18n` attributes (e.g., `i18n="chat.title|AI Assistant header"`). The `translationService.currentLang()` in `app.html` suggests a custom language switcher, but no `$localize` tag or translation pipe is used in the component templates.

The project's AGENTS.md specifies "i18n — @angular/localize, bilingual EN/FR from Phase 1." The XLF files exist but have no runtime effect.

**Fix:** Either:
1. Add `i18n` attributes to all template text and use Angular's built-in i18n extraction/rendering pipeline, or
2. If using a custom translation service, wire it into the component templates with a translation pipe

---

_Reviewed: 2026-06-03T21:00:00Z_
_Reviewer: gsd-code-reviewer (standard depth)_
_Depth: standard_
