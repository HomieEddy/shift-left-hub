---
phase: 08-testing-ci-cd
plan: 08
subsystem: ci
tags: [husky, lint-staged, pre-commit, pre-push, git-hooks, quality-gates]

requires:
  - phase: 07-quality-polish-devops
    provides: Prettier + ESLint toolchain for frontend, mvn verify with Checkstyle/SpotBugs for backend
provides:
  - Pre-commit hook (Husky + lint-staged) running Prettier --write + ESLint --fix on staged frontend files
  - Pre-push hook running backend mvn verify -B + frontend pnpm lint + pnpm test
  - lint-staged configuration for frontend TS/HTML/JSON/CSS file patterns
affects: [08-ci-cd-pipeline]

tech-stack:
  added: ["husky@^9.1.7", "lint-staged@^15.5.2"]
  patterns:
    - "Husky v9 with hooksPath=.husky/_ for local quality gates"
    - "lint-staged v15 via JSON config file for staged-file-specific commands"

key-files:
  created:
    - ".husky/pre-commit"
    - ".husky/pre-push"
    - ".husky/_/.gitignore"
    - ".lintstagedrc.json"
    - "package.json"
    - "package-lock.json"
  modified:
    - ".gitignore"

key-decisions:
  - "Use pnpm exec in lint-staged commands for Windows cross-platform compatibility (cmd.exe doesn't resolve node_modules/.bin)"
  - "lint-staged config in separate .lintstagedrc.json (not embedded in package.json) for cleaner separation"
  - "Husky v9 with hooksPath=.husky/_ directory convention (auto-set by husky init)"
  - "Pre-commit runs lint-staged which calls pnpm exec prettier --write + pnpm exec eslint --fix"

patterns-established:
  - "Local quality gates: pre-commit for fast formatting/linting (<5s), pre-push for full verification suite"
  - "Shell scripts in .husky/ with execute bit set for git hook execution"
  - "Root package.json as orchestration layer for both backend and frontend scripts"

requirements-completed: [TST-05]

duration: 22min
completed: 2026-06-08
---

# Phase 8 Plan 8: Pre-commit & Pre-push Hooks Summary

**Husky v9 + lint-staged local quality gates — pre-commit runs Prettier/ESLint via lint-staged, pre-push blocks on backend mvn verify or frontend lint/test failures**

## Performance

- **Duration:** 22 min
- **Started:** 2026-06-08T15:40:00Z
- **Completed:** 2026-06-08T16:02:00Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments

- Root `package.json` created with Husky (`prepare` script), lint-staged, and orchestration scripts (`lint:frontend`, `test:frontend`, `test:backend`, `format:check`)
- `.husky/pre-commit` configured to call `npx lint-staged` which runs Prettier + ESLint on staged frontend source files
- `.husky/pre-push` configured with full verification suite: backend `mvn verify -B`, frontend `pnpm lint`, frontend `pnpm test`
- `.lintstagedrc.json` with pattern-matched commands for `.ts`, `.html`, `.json`, `.css` files
- `.auth/` added to `.gitignore` for root-level auth artifacts
- Husky v9 initialized with `npx husky init` — generated `.husky/_/` helper scripts, set `core.hooksPath=.husky/_`
- Hooks verified working via `npx husky run pre-commit`

## Task Commits

Each task was committed atomically:

1. **Task 1: Initialize Husky and create pre-commit hook with lint-staged** - `751131f` (feat)
2. **Task 2: Create pre-push hook with full verification suite** - `fb26733` (feat)
3. **Task 3: Install dependencies and validate hooks** - `6504bc1` (chore)

## Files Created/Modified

- `.husky/pre-commit` - Pre-commit hook calling `npx lint-staged` for fast formatting/linting
- `.husky/pre-push` - Pre-push hook running backend `mvn verify -B` + frontend `pnpm lint && pnpm test`
- `.husky/_/.gitignore` - Husky v9 internal directory ignore (ignore generated helpers, track the .gitignore)
- `.lintstagedrc.json` - lint-staged v15 configuration with file pattern → command mappings
- `package.json` - Root package.json with `prepare` script, lint-staged, and orchestration scripts
- `package-lock.json` - Lockfile for root npm dependencies
- `.gitignore` - Added `.auth/` entry for root-level auth artifacts

## Decisions Made

- **Cross-platform command resolution:** Used `pnpm exec` prefix in lint-staged commands instead of bare `prettier`/`eslint` — on Windows, cmd.exe (used by lint-staged via execa) doesn't resolve `node_modules/.bin` binaries without npx/pnpm exec
- **Separate linst-staged config:** Chose `.lintstagedrc.json` over embedding in `package.json` — keeps the root package.json cleaner and the config is independently lint-staged-documented
- **Husky v9 hooksPath convention:** Let `npx husky init` set `core.hooksPath=.husky/_` — follows Husky v9 best practices for the internal wrapper architecture

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Replaced bare prettier/eslint with pnpm exec for Windows cmd.exe compatibility**
- **Found during:** Task 3 (Install dependencies and validate hooks)
- **Issue:** lint-staged runs commands via `cmd.exe` on Windows which doesn't have `node_modules/.bin` on PATH — `prettier --write` failed with "The filename, directory name, or volume label syntax is incorrect"
- **Fix:** Changed `prettier --write` to `pnpm exec prettier --write` and `eslint --fix` to `pnpm exec eslint --fix` in `.lintstagedrc.json`
- **Files modified:** `.lintstagedrc.json`
- **Verification:** `npx husky run pre-commit` with staged files passes cleanly
- **Committed in:** `6504bc1` (Task 3 commit)

**2. [Rule 3 - Blocking] Husky init overwrote pre-commit hook with default `npm test`**
- **Found during:** Task 3 (Install dependencies and validate hooks)
- **Issue:** `npx husky init` regenerated `.husky/pre-commit` with `npm test` content, replacing our `npx lint-staged`
- **Fix:** Restored `.husky/pre-commit` with `npx lint-staged` after husky init completed
- **Files modified:** `.husky/pre-commit`
- **Verification:** Verified pre-commit content via `Get-Content .husky/pre-commit`
- **Committed in:** `6504bc1` (Task 3 commit)

---

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking)
**Impact on plan:** Both necessary for correct operation on the Windows development environment. No scope creep.

## Issues Encountered

- **Husky v9 on Windows with Git Bash:** The `.husky/_/` helper scripts use `#!/usr/bin/env sh` shebangs which require Git Bash's `sh.exe` (available at `C:\Program Files\Git\bin\sh.exe`). Git for Windows resolves this via its built-in POSIX layer — hooks work correctly when `sh.exe` is findable from the Git installation.
- **lint-staged v17 ship:** `npm install --save-dev lint-staged@latest` installed v17.0.7 which has a `node >=22.22.1` engine requirement (we have v22.14.0) and changed CLI options. Pinned to `^15.0.0` per plan specification.

## User Setup Required

None - no external service configuration required. Husky hooks activate automatically after `npm install` (via the `prepare` script). Developers on Windows need Git for Windows installed (standard for git usage) for the hook shell scripts to resolve correctly.

## Next Phase Readiness

- Pre-commit and pre-push hooks are active and enforce code quality before commits/pushes
- Next step (Plan 08-09 CI Pipeline) will reference pre-push's verification suite as the CI gate
- `.husky/pre-push` commands (`mvn verify -B`, `pnpm lint`, `pnpm test`) match the planned CI pipeline steps

## Self-Check: PASSED

- [x] `.husky/pre-commit` exists with `npx lint-staged`
- [x] `.husky/pre-push` exists with `mvn verify`, `pnpm lint`, `pnpm test`
- [x] `.husky/_/.gitignore` exists
- [x] `.lintstagedrc.json` exists with Prettier + ESLint config
- [x] `package.json` has husky + lint-staged in devDependencies
- [x] `.gitignore` has `.auth/` entry
- [x] `751131f` committed (Task 1)
- [x] `fb26733` committed (Task 2)
- [x] `6504bc1` committed (Task 3)
- [x] `npx husky run pre-commit` passes (exit code 0)
- [x] SUMMARY.md exists with all required sections

---
*Phase: 08-testing-ci-cd*
*Completed: 2026-06-08*
