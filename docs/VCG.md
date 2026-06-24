# Shift-Left Knowledge Hub - Version Control Guidelines (VCG)

> **Updated:** 2026-06-23 — v2.2.
> Default branch is `master` (not `main`). Commit scopes are module names (`auth`, `kb`, `ticket`, etc.), not layers. Commitlint is enforced via `.husky/commit-msg`.

## 1. Branching Strategy (GitHub Flow)

To maintain a clean and deployable codebase, this project uses a simplified, continuous delivery branching model (GitHub Flow).

- **`master`:** The single source of truth. Code in `master` must **always** be in a deployable state. Direct commits to `master` are strictly prohibited.
- **Feature Branches:** All work happens in short-lived feature branches created from `master`.

### Branch Naming Convention

`<type>/<short-description>` — used for any non-phase work, OR `<type>/phase-N-short-description` for milestone-aligned contributions.

**Types:**

- `feat/` — A new feature or product addition (e.g. `feat/agent-dashboard-ui`)
- `fix/` — A bug fix (e.g. `fix/jwt-token-expiration`)
- `chore/` — Maintenance, dependencies, or configuration changes (e.g. `chore/update-angular-v21`)
- `docs/` — Documentation updates (e.g. `docs/align-with-v2.2`)
- `refactor/` — Code changes that neither fix a bug nor add a feature (e.g. `refactor/ticket-service-logic`)
- `test/` — Adding or fixing tests

---

## 2. Commit Message Standards (Conventional Commits)

This project enforces the **Conventional Commits** specification via `commitlint.config.cjs` and a `.husky/commit-msg` hook. Every commit must follow the format below or the commit will be rejected locally.

### Format

`<type>(<scope>): <subject>`

### Rules

1. **Type:** Must be one of `feat`, `fix`, `refactor`, `test`, `docs`, `chore`, `style`, `perf`.
2. **Scope:** Module or area of the monorepo (e.g. `auth`, `kb`, `ticket`, `ai`, `docker`, `deps`, `frontend`, `backend`). The project uses **module names**, not layers like `(ui)` / `(api)`.
3. **Subject:** Imperative present tense ("add feature", not "added feature"). No capital first letter. No trailing period. ≤100 chars.

### Examples

- `feat(kb): add article slug uniqueness check` (good)
- `fix(ticket): prevent null pointer on empty context` (good)
- `chore(deps): bump JJWT to 0.13.0` (good)
- `Added the new search thing to the backend.` (bad — not conventional, past tense, capitalized, ends in period)
- `feat(ui): tweak the button` (acceptable but prefer `feat(<module>):`)

---

## 3. Pull Request (PR) Lifecycle

Code merges into `master` strictly via Pull Requests.

### A. The "Self-Review" Rule

Before opening a PR or assigning a reviewer, the author **must** review their own "Files Changed" tab in GitHub.

- Check for accidentally committed `console.log()` or `System.out.println()`.
- Ensure no commented-out block of code is left behind.
- Verify that only files relevant to the feature are included.
- No `.planning/` files in the diff (those are gitignored).

### B. PR Template (`.github/pull_request_template.md`)

Every PR description must use the project template, which requires:

- **What this PR does** (1-2 sentences)
- **Requirements** (REQ-IDs from `REQUIREMENTS.md` or phase numbers from `ROADMAP.md`)
- **Decisions** (any non-obvious trade-offs)
- **Test counts** (e.g. `Backend: 469 (was 459, +10). Frontend: 306 (unchanged).`)
- **Checklist** (4 items: self-review, no secrets, no `.planning/`, branch rebased)

### C. Merging Rules

- **Squash and Merge:** When merging a PR into `master`, always use "Squash and Merge". The squash commit message becomes the single Conventional Commits entry for the feature on the `master` timeline.
- **Delete the feature branch** immediately after merging (use `gh pr merge --delete-branch` or the GitHub UI toggle) to keep the repository clean.
- **Never merge directly to `master`**, even for solo development.
- **Conflicts:** Rebase the feature branch onto `master` and resolve locally; never resolve via the GitHub UI.
