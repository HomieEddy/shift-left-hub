# Shift-Left Knowledge Hub — Project Guide

## Project Context

- **Project:** Shift-Left Knowledge Hub (SLKH)
- **Core Value:** Shift resolution as close to the user as possible by intercepting Level 0/1 issues before they reach the queue, while eliminating the documentation burden on IT agents.
- **Stack:** Angular 21.2 + Spring Boot 4.0.6 + PostgreSQL 16 (pgvector) + Spring AI 1.1.7
- **Architecture:** Modular Monolith (package-by-module)

## Current State

**Milestone:** v2.2 Post-Cleanup Polish — **shipped 2026-06-23**
**Test counts:** 469 backend (JUnit + Testcontainers) + 306 frontend (Vitest) + 10 E2E (Playwright)
**Last 6 PRs:** #126-#131 — see `.planning/milestones/v2.2-SUMMARY.md` for the full record.

The project is in steady state. New work should be planned as additional GSD phases or v2.3 tickets.

## GSD Workflow Rules

This project uses the GSD (Get Shit Done) workflow for phase planning. Follow these rules when picking up a new phase:

### Before Any Work

1. **Read STATE.md** (gitignored, lives at `.planning/STATE.md`) — current phase and progress
2. **Read ROADMAP.md** — phase structure and success criteria
3. **Read REQUIREMENTS.md** — requirements with traceability
4. **Check `.planning/`** for any active phase working files

### During Work

1. **Follow the phase plan** — execute plans in order within the current phase
2. **Stay in scope** — don't implement requirements from later phases
3. **Update STATE.md** — mark progress after each completed plan
4. **Phase artifacts** — create `PHASE-N/` directory with `PLAN.md` and execution artifacts

### After Phase Completion

1. Mark phase complete in STATE.md
2. Update REQUIREMENTS.md traceability (mark requirements as Complete)
3. Write a milestone summary under `.planning/milestones/`
4. Run `/gsd-plan-phase N+1` to start next phase (or `/gsd-new-milestone` for a fresh cycle)

## Version Control Rules

### Commits
- **Atomic commits** — each commit is one logical change. Not "WIP" or "fix stuff". A reader should understand exactly what changed from the message alone.
- **Message format:** `type(scope): short description` — e.g. `feat(auth): add JWT refresh rotation`, `fix(ticket): prevent null pointer on empty context`, `chore(deps): bump JJWT to 0.13.0`
  - Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `style`, `perf`
  - Scope: module or area (e.g. `auth`, `kb`, `ticket`, `ai`, `docker`, `deps`)
  - Subject: imperative present tense, no capital first letter, no trailing period, ≤100 chars
- **Commitlint** is enforced via the `.husky/commit-msg` hook (see `commitlint.config.cjs`).
- **Never commit secrets** — check for API keys, passwords, tokens in staged files before committing
- **Never skip hooks** — do not use `--no-verify` or `-n` unless explicitly instructed
- **Never force-push** — no `git push --force` or `git push --force-with-lease` unless recovering from a leaked secret
- **Never amend pushed commits** — rebase/amend only on local branches before push

### Branches
- **Default branch:** `master` — always stable, always deployable
- **Branch naming:** `type/phase-N-short-description` or `type/scope-short-description` — e.g. `feat/phase-14-seeding-revamp`, `fix/tier15-frontend-bugs`, `chore/infra-tier17`
- **Branch lifecycle:** create from `master`, commit work, open PR, merge back, delete branch (auto-deleted by `gh pr merge --delete-branch`)
- **One branch per logical unit** — do not bundle unrelated changes in a single branch
- **Keep branches short-lived** — rebase onto `master` if it drifts, avoid long-running branches

### Pull Requests
- **PR title:** Same convention as commit messages — `type(scope): short description`
- **PR body:** Use `.github/pull_request_template.md` — must include what the PR does, REQ-IDs, decisions, and test counts
- **Review your own diff first** — inspect all files before opening or requesting review
- **Keep PRs focused** — one phase or one feature per PR. No bundled scope.
- **Draft PRs** for work-in-progress. Convert to ready when complete.

### Merges
- **Merge strategy:** Squash merge into `master` — keeps a clean linear history. The squash commit message becomes the single entry for the feature.
- **Never merge directly to `master`** — always through a PR, even for solo development
- **Delete branch after merge** — keeps the remote clean
- **Conflicts:** Rebase the feature branch onto `master` and resolve locally, never resolve via GitHub UI

## Key Architecture Decisions

- **Modular Monolith** — package-by-module (`user/`, `article/`, `ticket/`, `ai/`, `workspace/`, etc.), not package-by-layer
- **Auth** — JWT with HttpOnly cookies + refresh rotation (no localStorage)
- **Database** — PostgreSQL 16 with pgvector extension, persistent tsvector + GIN index for FTS
- **AI** — Hybrid search (FTS + pgvector + RRF) with similarity threshold > 0.65; SSRF-safe endpoint validator
- **i18n** — `@angular/localize`, bilingual EN/FR from Phase 1
- **KCS** — Event-driven pipeline via Spring `ApplicationEventPublisher` (no Kafka)
- **Routing** — `canMatch` guards (not `canActivate`) so unauthorized navigations skip the lazy chunk
- **Observability** — `/actuator/{health,info,prometheus,metrics}` exposed; Micrometer Prometheus registry

## Code Guidelines (from docs/CCG.md)

### General Principles
- **SOLID** — Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, Dependency Inversion
- **KISS** — Code reads like well-structured English. No clever one-liners.
- **YAGNI** — No abstractions for use cases that don't exist yet. Build for today.
- **DRY** — Third copy-paste → extract to shared utility.
- **Boy Scout Rule** — Leave the codebase cleaner than you found it.

### Spring Boot Backend Standards (4.x)
- **Layered Architecture** — Controllers (HTTP/routing only, zero business logic) → Services (all business logic, `@Transactional`) → Repositories (database only)
- **DTO Pattern** — Never expose JPA Entities directly to API. Use Java 14+ `record` types for immutable Request/Response DTOs.
- **Constructor Injection** — Ban field injection (`@Autowired` on variables). Always use `@RequiredArgsConstructor` + `private final`.
- **Global Exception Handling** — No try-catch in Controllers. Throw custom domain exceptions from Service layer. Single `@RestControllerAdvice` (`GlobalExceptionHandler`) translates to RFC 7807 `ProblemDetail` responses.
- **Typed Exceptions** — Domain exceptions carry semantic meaning (`InvitationNotFoundException` → 404, `LastAdminException` → 409, `SelfModificationException` → 409, `AdminNotFoundException` → 401). New exceptions get a handler in `GlobalExceptionHandler`.

### Angular Frontend Standards (21.x)
- **Standalone components** — no NgModules for new code; signal-based state
- **Routing** — `canMatch` guards (not `canActivate`)
- **RxJS Subscriptions** — Prefer `async` pipe in templates. If `.subscribe()` is unavoidable in TS, use `takeUntilDestroyed()` (Angular 16+).
- **Smart vs Dumb Components** — Smart (features): inject services, hold state, pass data down. Dumb (shared): `@Input()` / `@Output()` only, no service dependencies.
- **Strong Typing** — Ban `any`. Define Interfaces/Types for all payloads. Use `unknown` when shape is uncertain (forces type-check before access).
- **Keep Logic Out of Templates** — No complex expressions in `{{ }}`. Calculate in TS or use Pipes.
- **File Naming** — Strict kebab-case per Angular Style Guide: `ticket-list.component.ts`, `ticket.service.ts`, `ticket.model.ts`

## Testing Guidelines (from docs/TSD.md)

High-ROI testing only — no trivial tests (getters/setters, visual component snapshots).

### Backend Testing (JUnit 5 + Mockito + Testcontainers)
- **Unit test only the Service layer** — Controllers have zero business logic, skip them.
- **Mock all external dependencies** (Repositories, `ChatClient`) via `@ExtendWith(MockitoExtension.class)`.
- **Never call real AI APIs in tests** — mock AI responses for deterministic, fast, cost-free tests.
- **Integration tests use Testcontainers** — never H2 (doesn't support JSONB/TSVECTOR). Real PostgreSQL 16 in ephemeral Docker containers (image `pgvector/pgvector:0.8.0-pg16`).
- **Test directory structure** mirrors the module structure: `user/`, `article/`, `ticket/`, `ai/`.

### Frontend Testing (Vitest + TestBed)
- **Test only Smart components and Services** — skip Dumb component presentation tests.
- **Use `HttpTestingController`** to mock API responses — never hit the real network.
- **Test RxJS logic** — debounce on search input, stream transformations, error handling.
- **Prefer `async` pipe in templates** — avoid `.subscribe()` where possible.
- **Run tests:** `pnpm test -- --watch=false`

### E2E Testing (Playwright)
- **One Golden Path** script at `e2e/playwright/golden-path.spec.ts` covering
  the critical happy path: `login → AI query → escalate to human → agent receives ticket`.
  Uses two browser contexts (User + Agent in the same test).
- **Exploratory specs** (admin, agent-dashboard, ai-chat, auth, document-ingestion,
  escalation, kb, workspace-management) live under `e2e/tests/*.spec.ts` and
  cover feature-specific flows. These are non-gating (run on demand) and are
  allowed to be added as needed.
- The Golden Path is the only E2E test the CI is required to pass.

### CI/CD Gatekeeping
- **Backend CI:** `mvn test` must pass (JUnit + Testcontainers). Build fails → Railway blocks deploy.
- **Frontend CI:** `pnpm test -- --watch=false` must pass. Vercel aborts deploy on failure.
- **Post-Deploy:** Optional Playwright health check against live domain.

## Critical GOTCHAs

- **Spring Boot is 4.0.6** (not 3.x). The Spring Initializr default is still 3.x — use this version explicitly.
- **Angular 21.2 is the current LTS** — Angular 19 and earlier are EOL.
- **Spring AI BOM must be 1.1.7** (not 1.0.0-M1).
- **Docker image must be `pgvector/pgvector:0.8.0-pg16`** (not `postgres:16-alpine`).
- **JJWT must be 0.13.0** (not 0.12.5).
- **Always set `similarityThreshold > 0.65`** in Spring AI RAG (validation floor in `AiConfigRequest`).
- **Always use persistent tsvector column + GIN index** for FTS (never compute at query time).
- **`.planning/` is gitignored** — never commit anything under that directory.
- **`pnpm` (not `npm`)** is the package manager — `pnpm-lock.yaml` is the source of truth.
