# Shift-Left Knowledge Hub — Project Guide

## Project Context

- **Project:** Shift-Left Knowledge Hub (SLKH)
- **Core Value:** Shift resolution as close to the user as possible by intercepting Level 0/1 issues before they reach the queue, while eliminating the documentation burden on IT agents.
- **Stack:** Angular 21 + Spring Boot 3.5 + PostgreSQL 16 (pgvector) + Spring AI
- **Architecture:** Modular Monolith

## GSD Workflow Rules

This project uses the GSD (Get Shit Done) workflow. Follow these rules:

### Before Any Work

1. **Read STATE.md** — understand current project state and current phase
2. **Read ROADMAP.md** — understand phase structure and success criteria
3. **Read REQUIREMENTS.md** — understand requirements with traceability
4. **Check .planning/** for any active phase working files

### During Work

1. **Follow the phase plan** — execute plans in order within the current phase
2. **Stay in scope** — don't implement requirements from later phases
3. **Update STATE.md** — mark progress after each completed plan
4. **Phase artifacts** — create PHASE-N/ directory with PLAN.md and execution artifacts

### After Phase Completion

1. Mark phase complete in STATE.md
2. Update REQUIREMENTS.md traceability (mark requirements as Complete)
3. Run `/gsd-plan-phase N+1` to start next phase

## Version Control Rules

### Commits
- **Atomic commits** — each commit is one logical change. Not "WIP" or "fix stuff". A reader should understand exactly what changed from the message alone.
- **Message format:** `type(scope): short description` — e.g. `feat(auth): add JWT refresh rotation`, `fix(ticket): prevent null pointer on empty context`, `chore(deps): bump JJWT to 0.13.0`
  - Types: `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `style`
  - Scope: module or area (e.g. `auth`, `kb`, `ticket`, `ai`, `docker`, `deps`)
- **Never commit secrets** — check for API keys, passwords, tokens in staged files before committing
- **Never skip hooks** — do not use `--no-verify` or `-n` unless explicitly instructed
- **Never force-push** — no `git push --force` or `git push --force-with-lease` unless recovering from a leaked secret
- **Never amend pushed commits** — rebase/amend only on local branches before push

### Branches
- **Default branch:** `main` — always stable, always deployable
- **Branch naming:** `type/phase-N-short-description` — e.g. `feat/phase-1-auth`, `fix/phase-3-search`, `chore/phase-6-deps`
- **Branch lifecycle:** create from `main`, commit work, open PR, merge back, delete branch
- **One branch per logical unit** — do not bundle unrelated changes in a single branch
- **Keep branches short-lived** — rebase onto `main` if it drifts, avoid long-running branches

### Pull Requests
- **PR title:** Same convention as commit messages — `type(scope): short description`
- **PR body:** Must include:
  - What this PR does (1-2 sentences)
  - Which requirement(s) it addresses (REQ-ID references)
  - Any decisions made during implementation
- **Review your own diff first** — inspect all files before opening or requesting review
- **Keep PRs focused** — one phase or one feature per PR. No bundled scope.
- **Draft PRs** for work-in-progress. Convert to ready when complete.

### Merges
- **Merge strategy:** Squash merge into `main` — keeps a clean linear history. The squash commit message becomes the single entry for the feature.
- **Never merge directly to `main`** — always through a PR, even for solo development
- **Delete branch after merge** — keeps the remote clean
- **Conflicts:** Rebase the feature branch onto `main` and resolve locally, never resolve via GitHub UI

## Key Architecture Decisions

- **Modular Monolith** — package-by-module (user/, article/, ticket/, ai/), not package-by-layer
- **Auth** — JWT with HttpOnly cookies + refresh rotation (no localStorage)
- **Database** — PostgreSQL 16 with pgvector extension, persistent tsvector + GIN index for FTS
- **AI** — Hybrid search (FTS + pgvector + RRF) with similarity threshold > 0.65
- **i18n** — `@angular/localize`, bilingual EN/FR from Phase 1
- **KCS** — Event-driven pipeline via Spring `ApplicationEventPublisher` (no Kafka)

## Current Phase

**Phase:** 0 — Project Initialized
**Status:** Ready for Phase 1

Next: `/gsd-plan-phase 1`

## Code Guidelines (from CCG.md)

### General Principles
- **SOLID** — Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, Dependency Inversion
- **KISS** — Code reads like well-structured English. No clever one-liners.
- **YAGNI** — No abstractions for use cases that don't exist yet. Build for today.
- **DRY** — Third copy-paste → extract to shared utility.
- **Boy Scout Rule** — Leave the codebase cleaner than you found it.

### Agent Skills (contributor setup)
Two MIT-licensed skills enforce the principles above at agent time. They are **not vendored in this repo** — each contributor installs them once into their own opencode config (per-user, no shared state, no hooks committed).

Install both (one-time per machine):

```
npx skills add DietrichGebert/ponytail
npx skills add shadcn/improve
```

- **ponytail** (`/ponytail`, `/ponytail-review`, `/ponytail-audit`, `/ponytail-debt`) — always-on YAGNI/KISS enforcement injected into every system prompt. Reaches for stdlib/native platform features before reaching for a new dependency. Never touches validation, security, error handling, or accessibility. Default mode is `full`; override per-session with `/ponytail lite|full|ultra|off` or `PONYTAIL_DEFAULT_MODE` env var. Source: <https://github.com/DietrichGebert/ponytail>.
- **improve** (`/improve`, `/improve quick|deep|security|branch|next|plan|execute|reconcile`) — on-demand read-only auditor. Maps the repo, audits across nine categories (correctness, security, performance, test coverage, tech debt, dependencies, DX, docs, direction), and writes self-contained plans to `plans/` for cheap executors. Never touches source. Use `/improve branch` before opening a PR, `/improve reconcile` between phases. Source: <https://github.com/shadcn/improve>.

These complement the existing KISS/YAGNI/DRY rules — stronger enforcement at agent time, not a replacement. Do not add their skill files, hooks, or plugins to this repository.

### Spring Boot Backend Standards
- **Layered Architecture** — Controllers (HTTP/routing only, zero business logic) → Services (all business logic, `@Transactional`) → Repositories (database only)
- **DTO Pattern** — Never expose JPA Entities directly to API. Use Java 14+ `record` types for immutable Request/Response DTOs.
- **Constructor Injection** — Ban field injection (`@Autowired` on variables). Always use `@RequiredArgsConstructor` + `private final`.
- **Global Exception Handling** — No try-catch in Controllers. Throw custom domain exceptions from Service layer. Single `@RestControllerAdvice` translates to standardized JSON error responses.

### Angular Frontend Standards
- **RxJS Subscriptions** — Prefer `async` pipe in templates. If `.subscribe()` is unavoidable in TS, use `takeUntilDestroyed()` (Angular 16+).
- **Smart vs Dumb Components** — Smart (features): inject services, hold state, pass data down. Dumb (shared): `@Input()` / `@Output()` only, no service dependencies.
- **Strong Typing** — Ban `any`. Define Interfaces/Types for all payloads. Use `unknown` when shape is uncertain (forces type-check before access).
- **Keep Logic Out of Templates** — No complex expressions in `{{ }}`. Calculate in TS or use Pipes.
- **File Naming** — Strict kebab-case per Angular Style Guide: `ticket-list.component.ts`, `ticket.service.ts`, `ticket.model.ts`

## Testing Guidelines (from TSD.md)

High-ROI testing only — no trivial tests (getters/setters, visual component snapshots).

### Backend Testing (JUnit 5 + Mockito + Testcontainers)
- **Unit test only the Service layer** — Controllers have zero business logic, skip them.
- **Mock all external dependencies** (Repositories, `ChatClient`) via `@ExtendWith(MockitoExtension.class)`.
- **Never call real AI APIs in tests** — mock AI responses for deterministic, fast, cost-free tests.
- **Integration tests use Testcontainers** — never H2 (doesn't support JSONB/TSVECTOR). Real PostgreSQL in ephemeral Docker containers.
- **Test directory structure** mirrors the module structure: `user/`, `article/`, `ticket/`, `ai/`.

### Frontend Testing (Jasmine + Karma/Jest)
- **Test only Smart components and Services** — skip Dumb component presentation tests.
- **Use `HttpTestingController`** to mock API responses — never hit the real network.
- **Test RxJS logic** — debounce on search input, stream transformations, error handling.
- **Prefer `async` pipe in templates** — avoid `.subscribe()` where possible.

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
- **Frontend CI:** `npm run test -- --watch=false` must pass. Vercel aborts deploy on failure.
- **Post-Deploy:** Optional Playwright health check against live domain.

## Critical GOTCHAs (from research)

- Angular 19 is EOL — use Angular 21.2+ LTS
- Spring AI BOM must be 1.1.7 (not 1.0.0-M1)
- Docker image must be pgvector/pgvector:0.8.0-pg16 (not postgres:16-alpine)
- JJWT must be 0.13.0 (not 0.12.5)
- Always set similarityThreshold > 0.65 in Spring AI RAG
- Always use persistent tsvector column + GIN index for FTS (never compute at query time)
