# Contributing to Shift-Left Knowledge Hub

Thanks for your interest in contributing. This document covers the
basics; for the full developer guide see `AGENTS.md` and the GSD
planning artifacts under `.planning/`.

## Workflow

1. **Pick an issue or phase.** Check `.planning/ROADMAP.md` for
   upcoming phases, or open a GitHub issue describing the change.
2. **Branch from master.** Naming convention:
   `type/phase-N-short-description` (e.g.
   `feat/phase-3-search`, `fix/tier15-frontend-bugs`).
3. **One logical change per branch.** Bundle unrelated work in
   separate PRs.
4. **Atomic commits.** Each commit message uses the form
   `type(scope): short description` — see
   `commitlint.config.cjs` for the enforced rules.
5. **Open a draft PR early.** Convert to "ready for review" once CI
   is green and you've self-reviewed the diff.
6. **Squash-merge into master.** Branch is deleted automatically
   after merge.

## Quality gates

CI is blocking. Before opening a PR, run the same checks locally:

```sh
# Backend
cd backend
./mvnw verify -B        # Checkstyle, SpotBugs, unit + integration tests

# Frontend
cd frontend
pnpm lint
pnpm test -- --watch=false
pnpm build
```

## Commit types

| Type       | Use for                                          |
|------------|--------------------------------------------------|
| feat       | New user-facing feature                          |
| fix        | Bug fix                                          |
| refactor   | Code change that neither fixes a bug nor adds a feature |
| test       | Adding or fixing tests                           |
| docs       | Documentation only                               |
| chore      | Tooling, deps, housekeeping                      |
| style      | Whitespace, formatting, missing semicolons       |
| perf       | Performance improvement                          |

## Code style

The project follows the standards in `AGENTS.md`:

- **SOLID, KISS, YAGNI, DRY**
- **Backend:** Java 21, Spring Boot 4, package-by-module,
  constructor injection only, typed exceptions mapped to RFC 7807
  problem details.
- **Frontend:** Angular 21, standalone components, signals for state,
  async pipe in templates where possible, no `any`, strict types.

## Testing policy

- **Backend:** unit test only the Service layer. Mock repositories
  and `ChatClient`. Integration tests use Testcontainers (no H2).
- **Frontend:** test Smart components and Services only. Mock HTTP
  with `HttpTestingController`.
- **E2E:** one Golden Path Playwright script at
  `e2e/playwright/golden-path.spec.ts` is the only E2E test CI
  requires. Other Playwright specs under `e2e/tests/` are
  exploratory and non-gating.

## Reporting security issues

See `SECURITY.md`. Please do not file public issues for
vulnerabilities.
