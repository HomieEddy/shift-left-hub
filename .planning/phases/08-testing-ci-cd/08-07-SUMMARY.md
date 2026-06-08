---
phase: 08-testing-ci-cd
plan: 07
subsystem: ci-cd
tags: [github-actions, sonarcloud, jacoco, maven, pnpm, eslint, testcontainers]

requires:
  - phase: 07-quality-polish-devops
    provides: Checkstyle, SpotBugs, ESLint static analysis toolchain
  - phase: 08-testing-ci-cd
    provides: Backend unit + integration tests (plans 01-03), frontend tests (plans 04-05)
provides:
  - GitHub Actions CI workflow running on every PR/push to master
  - Backend CI job with JDK 21, Maven cache, Testcontainers PostgreSQL, JaCoCo, SonarQube Cloud
  - Frontend CI job with Node 22, pnpm, frozen-lockfile, lint + test (coverage) + build
  - CODEOWNERS for branch protection context
affects: [08-08]

tech-stack:
  added: [github-actions, sonarcloud]
  patterns:
    - "Testcontainers-direct CI: no service container, Testcontainers starts pgvector directly on GitHub Actions runners"
    - "Dual CI jobs: backend (Java/Maven) and frontend (Node/pnpm) run in parallel"
    - "SonarQube Cloud as informational scan (continue-on-error: true)"

key-files:
  created:
    - .github/workflows/ci.yml — CI/CD pipeline (backend + frontend jobs, SonarQube scan)
    - .github/CODEOWNERS — Default repo ownership
  modified:
    - .lintstagedrc.json — Fixed commands for cross-platform Windows/macOS/Linux
    - .gitignore — Added .auth/ pattern, removed duplicate entry
    - .husky/pre-commit — Restored to original `npx lint-staged`

key-decisions:
  - "Testcontainers-direct approach: No PostgreSQL service container in CI. GitHub Actions runners have Docker, so Testcontainers starts pgvector directly. Cleaner — single source of truth matching local dev."
  - "SonarQube Cloud is informational only (continue-on-error: true). Checkstyle + SpotBugs remain the hard fail gates."
  - "CI runs on both PR and push to master/main — ensures branch protection gates can require the CI status check."

requirements-completed: [TST-05]

duration: 12min
completed: 2026-06-08
---

# Phase 8 Plan 7: CI/CD Pipeline (GitHub Actions)

**GitHub Actions CI pipeline for the full stack: backend (JDK 21, Maven, Testcontainers, JaCoCo, SonarQube) and frontend (Node 22, pnpm, lint, test, build), gating all PRs to master.**

## Performance

- **Duration:** 12 min
- **Started:** 2026-06-08T15:37:00Z
- **Completed:** 2026-06-08T15:52:45Z
- **Tasks:** 2
- **Files modified:** 5

## Accomplishments

- Created `.github/workflows/ci.yml` with parallel backend + frontend jobs
- Backend job: JDK 21 (Temurin), Maven cache, `./mvnw verify -B` (Checkstyle + SpotBugs + unit tests + integration tests + JaCoCo 80%/70% thresholds), SonarQube Cloud informational scan
- Frontend job: Node 22, pnpm 10, frozen-lockfile, ESLint, unit tests with coverage (ChromeHeadless), production build
- Created `.github/CODEOWNERS` with default ownership (`@shiftleft-admin`)
- Fixed pre-commit hook tooling for cross-platform Windows compatibility
- Integrated with existing JaCoCo coverage thresholds (80% line, 70% branch) from pom.xml

## Task Commits

Each task was committed atomically:

1. **Task 1: Create GitHub Actions CI workflow** - `a2ed629` (feat)
2. **Task 2: Create CODEOWNERS, validate YAML, add .auth/ to gitignore** - `cad8a4c`, `f544df0` (chore)

## Files Created/Modified

- `.github/workflows/ci.yml` — **Created.** Full CI pipeline with backend (Java/Maven/SonarQube) and frontend (Node/pnpm) parallel jobs. Triggers on PR + push to master/main. (95 lines)
- `.github/CODEOWNERS` — **Created.** Default repo ownership set to `@shiftleft-admin`.
- `.lintstagedrc.json` — **Modified.** Replaced Unix-only `cd frontend && prettier --write` with cross-platform `npx --prefix frontend prettier --write`. Added explicit ESLint config path `--config frontend/eslint.config.js`.
- `.husky/pre-commit` — **Modified.** Restored from a broken PowerShell script to original `npx lint-staged`.
- `.gitignore` — **Modified.** Simplified — removed duplicate `.auth/` entry and the `# Root auth dir` comment. The top-level `.auth/` pattern already covers all directories at any level.

## Decisions Made

- **Testcontainers-direct CI approach** — The plan discussed using a PostgreSQL service container vs Testcontainers directly. Following the plan's refined recommendation: no service container. GitHub Actions runners have Docker, so Testcontainers starts its own `pgvector/pgvector:0.8.0-pg16` container. This creates a single source of truth matching local development workflow.
- **SonarQube informational only** — `continue-on-error: true` ensures the SonarQube scan doesn't block the build, per D-02. Checkstyle and SpotBugs remain the hard fail gates.
- **CI triggers** — Running on both `pull_request` and `push` to master/main ensures the CI status check is available for branch protection rules.
- **pnpm lockfile integrity** — Using `--frozen-lockfile` in CI ensures no dependency drift (mitigates T-08-07-03 from the threat model).

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Pre-commit hook broken on Windows**
- **Found during:** Task 2 (commit attempt)
- **Issue:** `.lintstagedrc.json` used `cd frontend && prettier --write` which works on macOS/Linux but fails on Windows PowerShell. Additionally, `.husky/pre-commit` had been modified to a broken PowerShell one-liner that didn't actually run lint-staged.
- **Fix:** 
  - Changed `.lintstagedrc.json` commands from `cd frontend && command` to `npx --prefix frontend command` (cross-platform).
  - Added explicit `--config frontend/eslint.config.js` to ESLint command since `npx --prefix` doesn't change the working directory for config file resolution.
  - Restored `.husky/pre-commit` to original `npx lint-staged`.
- **Files modified:** `.lintstagedrc.json`, `.husky/pre-commit`
- **Verification:** Pre-commit hook runs lint-staged successfully, prettier formats staged files, ESLint no longer errors.
- **Committed in:** `cad8a4c`, `f544df0` (Task 2 commits)

---

**Total deviations:** 1 auto-fixed (1 blocking)
**Impact on plan:** Fix was necessary to enable git commits on Windows. No scope creep.

## Issues Encountered

- **Pre-existing staged files from previous sessions** — The working tree had files staged from prior plan executions (08-06 E2E tests). These caused lint-staged hooks to attempt formatting on frontend files, leading to cascading failures. Resolved by un staging unrelated files before committing.
- **Husky git stash/restore cycle** — When lint-staged failed, husky's error recovery stashed and restored the working tree, which repeatedly overwrote my `.lintstagedrc.json` edits. Required re-writing the file after each failed commit attempt.

## Threat Surface

All threat model mitigations have been applied:
- **T-08-07-01** (SONAR_TOKEN secret): The `secrets.SONAR_TOKEN` reference ensures the token is never exposed in CI logs or workflow YAML.
- **T-08-07-03** (pnpm lockfile integrity): `--frozen-lockfile` flag prevents dependency drift without a lockfile change.
- **T-08-07-02** (Docker supply chain) and **T-08-07-04** (workflow injection) were accepted per the plan.

No new threat surface introduced beyond what was modeled.

## User Setup Required

External services require manual configuration. See plan frontmatter (`user_setup`) for:
- **SONAR_TOKEN**: Add to GitHub repo secrets (`Settings → Secrets and variables → Actions`)
- **Branch protection**: Enable on `master` requiring CI status checks
- **Railway/Vercel**: Connect repo and enable "Wait for CI" deploy setting (if deployed)

## Next Phase Readiness

- CI pipeline ready for plan 08-08 (Playwright E2E CI integration or remaining wrap-up)
- All quality gates from Phase 7 (Checkstyle, SpotBugs, ESLint, JaCoCo) are enforced by the CI pipeline
- Husky pre-commit + lint-staged work correctly on Windows (cross-platform fix applied)

---

## Self-Check: PASSED

| Check | Result |
|-------|--------|
| `.github/workflows/ci.yml` exists | ✅ PASS |
| `.github/CODEOWNERS` exists | ✅ PASS |
| `.lintstagedrc.json` modified | ✅ PASS |
| `.husky/pre-commit` restored | ✅ PASS |
| Commit a2ed629 (Task 1) | ✅ `feat(ci): create GitHub Actions CI workflow` |
| Commit cad8a4c (Task 2) | ✅ `chore(ci): add CODEOWNERS and fix lint-staged` |
| YAML parses as valid | ✅ Parsed by js-yaml without errors |
| CI YAML name: "CI" | ✅ PASS |
| CI jobs: backend + frontend | ✅ PASS |
| Backend steps: 5 | ✅ PASS |
| Frontend steps: 8 | ✅ PASS |
| Contains `mvnw verify` | ✅ Line 41 |
| Contains `pnpm` | ✅ Lines 68, 82, 86, 90, 94 |
| Line count ≥ 80 | ✅ 95 lines |
| SonarQube: informational only | ✅ `continue-on-error: true` |
| JaCoCo thresholds: 80%/70% | ✅ Configured in pom.xml (Plan 08-01/02) |
| `.auth/` in `.gitignore` | ✅ Present at line 5 |

---

*Phase: 08-testing-ci-cd*
*Completed: 2026-06-08*
