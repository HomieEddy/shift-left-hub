# Cleanup Audit — Phase A Discovery

**Date:** 2026-06-21
**Scope:** 628 source files (194 Java, 110 TS, 62 tests)
**Method:** 4 parallel audit agents across 9 categories (correctness, security, performance, test coverage, tech debt, dependencies, DX, i18n, a11y)
**Verdict:** 80+ findings. Many HIGH-confidence, many S-effort. Significant cleanup opportunity.

---

## Tier 1 — Security (URGENT, act first)

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| S-1 | Path traversal: `..` survives filename sanitization → upload writes outside workspace dir | `backend/.../document/service/DocumentService.java:157-163` | S | HIGH | ✓ fixed in `fix/s-1-path-traversal-uploads` |
| S-2 | Real admin JWTs in `cookies.txt` (gitignored but unprotected on disk) | `cookies.txt:5-6` | S | HIGH |
| S-3 | `AiConfigController.getConfig` lacks `@PreAuthorize`; only `authenticated()` required → any USER can read AI config (incl. `hasOpenaiKey`) | `backend/.../ai/api/AiConfigController.java:32-35` | S | HIGH | ✓ fixed in `fix/s-3-aiconfig-preauthorize` |
| S-4 | `AiConfigController.testConnection` missing `@Valid` → `@Pattern`/`@NotBlank` silently bypassed | `backend/.../ai/api/AiConfigController.java:55-59` | S | HIGH |
| S-5 | `AdminWorkspaceLlmConfigController.testConnection` missing `@Valid` | `backend/.../llmconfig/api/AdminWorkspaceLlmConfigController.java:68-74` | S | HIGH |
| S-6 | Dev JWT signing secret literal in `.env` (forgery if copied to prod) | `.env:2` | S | HIGH |
| S-7 | JWT filter logs user email + role + workspace_id at INFO per request (PII) | `backend/.../config/SecurityConfig.java:196-199` | S | HIGH |
| S-8 | Rate limiter uses only `getRemoteAddr()`; ignores `X-Forwarded-For` → spoofable throttle key | `backend/.../config/RateLimitingFilter.java:44-52` | S | MED |
| S-9 | `MasterSeeder` reuses same password env value for ALL seed users (admin + non-admin) | `backend/.../common/config/MasterSeeder.java:152-160` | S | MED |
| S-10 | Hardcoded default DB password `shiftleft` in `application.properties` | `backend/src/main/resources/application.properties:7` | S | MED |
| S-11 | Hardcoded default AI encryption salt `ShiftLeftKBSalt` weakens PBKDF2 | `backend/src/main/resources/application.properties:49` | S | MED |
| S-12 | Outbound AI endpoint URL not host/IP allow-listed → SSRF to internal services | `backend/.../ai/service/AiConfigService.java:142-156` + `OpenAiCompatibleChatModel.java:100-113` | M | MED | ✓ fixed in `fix/s-12-endpoint-ssrf` — EndpointUrlValidator |
| S-13 | `OpenAiCompatibleEmbeddingModel` allows `http://` endpoint with Bearer header (MITM) | `backend/.../ai/service/OpenAiCompatibleEmbeddingModel.java:94-100` | S | MED | ✓ fixed in `fix/s-12-endpoint-ssrf` — same PR, buildRestClient refuses http+key |
| S-14 | Most `/api/admin/**` controllers rely solely on URL matchers, not `@PreAuthorize` (defense-in-depth gap) | `AdminKcsController`, `AdminCategoryController`, `AdminUserController`, `AdminTagController`, `AdminWorkspaceController`, `AdminArticleController` | M | MED | ✓ fixed in `fix/s-14-admin-preauthorize` — class-level @PreAuthorize on all 6 |
| S-15 | `KcsEventListener` logs AI-drafted article title (user content) at INFO | `backend/.../kcs/service/KcsEventListener.java:68` | S | MED |
| S-16 | JWT validation errors logged with `e.getMessage()` (may echo claim values) | `backend/.../config/JwtService.java:179,195` | S | MED |
| S-17 | `GlobalExceptionHandler` returns raw exception class + message + first stack frame in dev profile | `backend/.../common/config/GlobalExceptionHandler.java:316-327` | S | MED |

---

## Tier 2 — Banned patterns (per AGENTS.md)

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| B-1 | `@Autowired` annotation on constructor (redundant; auto-wires since Spring 4.3) | `backend/.../document/service/DocumentChunkingService.java:18` | S | HIGH |
| B-2 | try/catch inside `@RestController` SSE endpoint (must use `@RestControllerAdvice`) | `backend/.../ai/api/ChatController.java:54,60,76` | M | HIGH |
| B-3 | `.subscribe()` in component without `takeUntilDestroyed` | `frontend/.../tickets/escalation-form/escalation-form.component.ts:71` | S | HIGH |
| B-4 | `confirmationDialog.confirm(...).subscribe(...)` without `takeUntilDestroyed` (5 sites) | `workspace-members.component.ts:102,124,142`; `user-list.component.ts:129`; `tag-manager.component.ts:108,132` | S | HIGH |
| B-5 | 3-level ternary inside `{{ }}` template | `frontend/.../landing/landing.component.html:10` | S | HIGH |

---

## Tier 3 — i18n / a11y

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| I-1..14 | Hardcoded English error/placeholder/aria-label strings (14 sites across components) | `chat.component.ts:152`; `article-search.component.ts:128`; `article-list.component.ts:56`; `workspace-list.component.ts:59,84` (placeholders :30, :43); `taxonomy-tree.component.ts:56`; `taxonomy-bulk.component.ts:52,56`; `tag-manager.component.ts:96`; `document-list.component.ts:164,189,217`; `agent-ticket-detail.component.ts:110,137,161,195`; `app.html:454`; `workspace-switcher.component.html:51`; `skeleton.component.html:7`; `skeleton-table.component.html:4`; `skeleton-card.component.html:4`; `search-input.component.html:40`; `modal.component.html:12,23` | S each | HIGH |

---

## Tier 4 — Dead code (verbatim removal, S effort)

| # | Finding | file:line | Conf |
|---|---------|-----------|------|
| D-1 | `DocumentRepository documentRepository` field injected but never used in `UnifiedSearchService` | `backend/.../ai/service/UnifiedSearchService.java:4,25` | HIGH |
| D-2 | `EmbeddingService.generateEmbedding(String)` public, no callers | `backend/.../ai/service/EmbeddingService.java:47-49` | HIGH |
| D-3 | `EmbeddingService.storeEmbedding(Article)` public, no callers | `backend/.../ai/service/EmbeddingService.java:56-68` | HIGH |
| D-4 | `AiConfigService.buildChatClient(AiConfig)` overload unused (only `(String,String,String,String)` is called) | `backend/.../ai/service/AiConfigService.java:249-255` | HIGH |
| D-5 | `DocumentService.findByContentHash(UUID, String)` public method unused | `backend/.../document/service/DocumentService.java:357-362` | HIGH |
| D-6 | `WorkspaceLlmConfigResponse.from(...)` `return null;` branch unreachable | `backend/.../llmconfig/api/dto/WorkspaceLlmConfigResponse.java:44-47` | HIGH |
| D-7 | Unused imports in `AiConfigService`: `ChatCompletion`, `MetadataMode`, `OpenAiEmbeddingModel`, `OpenAiEmbeddingOptions` | `backend/.../ai/service/AiConfigService.java:4,16,23,24` | HIGH |
| D-8 | TODO: `KCS drafting failed` `RuntimeException` thrown twice in same method (first is dead branch) | `backend/.../kcs/service/KcsEventListener.java:113-114,118-119` | MED |

---

## Tier 5 — Duplication (extract to helpers)

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| X-1 | 60+ `withCredentials: true` options duplicated across 14 frontend services; `baseUrlInterceptor` already sets it for all `/api/*` | `frontend/.../services/*.ts` (47+ matches) | S | HIGH |
| X-2 | `RuntimeException("User not found")` repeated 7×; `UserNotFoundException` exists but unused | `WorkspaceController:40,62,80`; `InvitationController:37,54,71`; `AuthController:113` | S | HIGH |
| X-3 | `slug + "-" + UUID.randomUUID().toString().substring(0, 8)` slug-uniqueness pattern duplicated 4× | `ArticleService.java:104,142`; `DocumentService.java:252`; `KcsDraftingService.java:84` | S | HIGH |
| X-4 | `chat.service.ts` re-implements `__env` base-URL resolution using raw `fetch` (bypasses `baseUrlInterceptor`) | `frontend/.../chat/chat.service.ts:31-35` | M | HIGH |
| X-5 | `WorkspaceRoleService.fetchRole()` and `WorkspaceService.getMyRole()` both call same `GET /api/workspaces/current/role` | `workspace-role.service.ts:18-22`; `workspace.service.ts:98-102` | S | HIGH |
| X-6 | SSE-emitter try/catch + `emitter.send(SseEmitter.event()...)` boilerplate repeated 5× in one file | `backend/.../ai/service/AiChatService.java:73,97-101,119-128,136-144,152-157` | M | HIGH |
| X-7 | `slugify` regex chain duplicated 3× | `ArticleService.java:228-234`; `DocumentService.java:247`; `KcsDraftingService` (private) | S | MED |
| X-8 | `isOpenAiProvider(provider) && apiKey != null && !apiKey.isBlank()` repeated 3× in `AiConfigService` | `AiConfigService.java:147,226,273` | S | HIGH |
| X-9 | `KcsDraftingService` + `PublicArticleService` both unwrap the same 9-column `searchByText` `Object[]` row → DTO | `PublicArticleService.java:123-149`; `AiChatService.java:269-282` | M | MED |
| X-10 | `ChatResponseMetadata` + `ChatGenerationMetadata` builders near-identical in `call()` and `stream()` | `OpenAiCompatibleChatModel.java:48-55,70-78` | S | MED |
| X-11 | `tag.service.ts` / `article.service.ts` / `public-article.service.ts` near-identical CRUD with manual `withCredentials` | `frontend/.../kb/services/*.ts` | M | MED |
| X-12 | `WorkspaceLlmConfigService` + `AiConfigService.updateConfig` both do nullable-merge over similar field sets | `WorkspaceLlmConfigService.java:57-76`; `AiConfigService.java:107-124` | M | MED |
| X-13 | `chat.component.ts` builds escalation payload twice (signal + output emit) | `chat.component.ts:53-66,177-181` | S | MED |
| X-14 | `WorkspaceService.leaveWorkspace` / `removeMember` / `changeMemberRole` each call `countByIdWorkspaceIdAndRole` separately | `WorkspaceService.java:175,195,217` | S | LOW |

---

## Tier 6 — Performance / N+1

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| P-1 | `AgentTicketService.listTickets` `findAll()` + filter in Java + lazy `getUser()` per row | `AgentTicketService.java:58-67` | M | HIGH |
| P-2 | `CategoryService.getAllCategories` N+1 on `cat.getChildren().size()` | `CategoryService.java:39-41` | S | HIGH |
| P-3 | `CategoryService.getCategory`/`updateCategory`/`mergeCategories` same N+1 | `CategoryService.java:54,113,176` | S | HIGH |
| P-4 | `TagService.getAllTags` N+1 on `getArticleCount(tag)` per tag | `TagService.java:32-35` | S | HIGH |
| P-5 | `ArticleResponse.from` triggers `getTags()` + `getAuthor()` on every page row | `ArticleResponse.java:42-64` | M | MED |
| P-6 | `KcsDraftingService.checkDuplicates` consumes only `row[0]` but query returns 9 cols incl. `ts_headline(...)` | `KcsDraftingService.java:127-138` | S | MED |
| P-7 | `EmbeddingService.reEmbedAll` synchronous vector writes on @Async thread | `EmbeddingService.java:96-120` | M | HIGH |
| P-8 | `AiChatService.processChat` runs 4 hybrid-search queries sequentially before streaming | `AiChatService.java:59-80` | M | MED |
| P-9 | `KcsEventListener` holds DB connection during `Thread.sleep(backoff)` (up to 7s) | `KcsEventListener.java:91-120` | M | MED |
| P-10 | `DocumentEventListener.handleDocumentUploaded` entire ETL pipeline on single `kcsTaskExecutor` thread | `DocumentEventListener.java:35-86` | M | MED |
| P-11 | `WorkspaceService.generateUniqueSlug` O(N) roundtrips via `while (existsBySlug)` | `WorkspaceService.java:275-280` | S | LOW |

---

## Tier 7 — Dependencies

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| DEP-1 | `spring-modulith-starter-core` + `spring-modulith-starter-test` declared but `org.springframework.modulith.*` imported in **0** Java sources | `backend/pom.xml:114-129` | S | HIGH |
| DEP-2 | `Caffeine` is appropriate for `RateLimitingFilter` — KEEP (no change) | — | — | LOW |
| DEP-3 | `DOMPurify` is appropriate for the 1 sanitization site — KEEP (no change) | — | — | LOW |
| DEP-4 | `@angular/cdk` is already optimally imported via `@angular/cdk/dialog` subpath — KEEP | — | — | LOW |
| DEP-5 | Apache POI ooxml is heavy (30+ MB transitive) for 12 lines of `.docx` parsing — track but keep | `backend/pom.xml:80-84` | M | LOW |

---

## Tier 8 — Tests / Coverage

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| T-1 | `JwtService` has no unit test (critical security logic) | `backend/.../config/JwtService.java` | M | HIGH |
| T-2 | 9 frontend services without `*.spec.ts` (auth-token, workspace-role, llm-settings, category, agent-ticket, public-article, tag, confirmation-dialog, toast) | `frontend/.../services/*.ts` | M | MED |
| T-3 | AGENTS.md says "Exactly one Playwright script at `e2e/playwright/golden-path.spec.ts`" but 9 specs exist under `e2e/tests/` — doc/contract drift | `AGENTS.md:139-143` vs `e2e/tests/*.spec.ts` | M | HIGH |

---

## Tier 9 — Logging

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| L-1 | `console.error` / `console.warn` left in production paths (8 sites); `workspace-role.service.ts:27`, `error.interceptor.ts:41`, `main.ts:7` are unguarded | various | S | HIGH |
| L-2 | (None found) System.out / System.err in Java source | — | — | HIGH |

---

## Tier 10 — Oversized files / class responsibilities

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| O-1 | `DocumentService` 329 lines, 6 responsibilities (CRUD, upload, status, search, paths, chunks) | `DocumentService.java:1-329` | L | MED |
| O-2 | `KcsDraftingService` 356 lines, 4 responsibilities (prompt, LLM, parse, dedup) | `KcsDraftingService.java:1-356` | M | MED |
| O-3 | `MasterSeeder` 316 lines, 5 distinct seeding steps | `MasterSeeder.java:104-305` | M | MED |

---

## Tier 11 — TODOs / Stub code (deferred by design)

| # | Finding | file:line | Effort | Conf |
|---|---------|-----------|--------|------|
| ST-1 | TODO: Resolve {domain}/{categories} from workspace config — current code replaces with empty string | `backend/.../ai/service/AiChatService.java:344-347` | M | HIGH |
| ST-2 | TODO: When wiring AI context in Phase 4, include full transcript — current code injects fake user message | `frontend/.../chat/chat.component.ts:189-195` | M | HIGH |

---

## Summary by category

- **Security:** 17 findings, 7 HIGH
- **Banned patterns:** 5 findings, 5 HIGH
- **i18n / a11y:** 14 sites (mostly i18n), all HIGH
- **Dead code:** 8 findings, 7 HIGH
- **Duplication:** 14 findings, 6 HIGH
- **Performance / N+1:** 11 findings, 4 HIGH
- **Dependencies:** 1 high-confidence orphan (Spring Modulith)
- **Tests:** 3 findings, 2 HIGH
- **Logging:** 1 finding, HIGH
- **Oversized:** 3 findings, MED
- **Stubs:** 2 findings (deferred by design — leave for planned phases)

**Estimated total cleanup effort:** ~30 PRs across 5 atomic commit groups.
