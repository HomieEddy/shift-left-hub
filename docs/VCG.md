# Shift-Left Knowledge Hub - Version Control Guidelines (VCG)

## 1. Branching Strategy (GitHub Flow)
To maintain a clean and deployable codebase, this project uses a simplified, continuous delivery branching model (GitHub Flow). 

* **`main`:** The single source of truth. Code in `main` must **always** be in a deployable state. Direct commits to `main` are strictly prohibited.
* **Feature Branches:** All work happens in short-lived feature branches created from `main`.

### Branch Naming Convention
Branches must be named purposefully using the following format: `<type>/<short-description>`

**Types:**
* `feat/` - A new feature or product addition (e.g., `feat/agent-dashboard-ui`)
* `fix/` - A bug fix (e.g., `fix/jwt-token-expiration`)
* `chore/` - Maintenance, dependencies, or configuration changes (e.g., `chore/update-angular-v17`)
* `docs/` - Documentation updates (e.g., `docs/update-api-readme`)
* `refactor/` - Code changes that neither fix a bug nor add a feature (e.g., `refactor/ticket-service-logic`)

---

## 2. Commit Message Standards (Conventional Commits)
This project enforces the **Conventional Commits** specification. This creates a readable history and allows for automated semantic versioning and changelog generation.

### Format
`<type>(<scope>): <subject>`

### Rules
1. **Type:** Must be one of `feat`, `fix`, `chore`, `docs`, `refactor`, `style`, `test`.
2. **Scope (Optional but recommended):** Because this is a monorepo, always indicate which part of the app the commit affects: `(ui)`, `(api)`, `(db)`, or `(infra)`.
3. **Subject:** Use the imperative, present tense ("add feature" not "added feature"). Do not capitalize the first letter. Do not end with a period.

### Examples
* ✅ `feat(api): implement postgres full-text search for articles`
* ✅ `fix(ui): resolve overflow issue on ticket escalation form`
* ✅ `chore(infra): add pgadmin to docker-compose`
* ❌ `Added the new search thing to the backend.` *(Bad: not conventional, past tense, capitalized, ends in period)*

---

## 3. Pull Request (PR) Lifecycle
Code merges into `main` strictly via Pull Requests. 

### A. The "Self-Review" Rule
Before opening a PR or assigning a reviewer, the author **must** review their own "Files Changed" tab in GitHub/GitLab. 
* Check for accidentally committed `console.log()` or `System.out.println()`.
* Ensure no commented-out block of code is left behind.
* Verify that only files relevant to the feature are included.

### B. Standardized PR Template
Every PR description must use the following structure to provide context to reviewers:

```markdown
## Description
## Type of Change
- [ ] Bug fix (non-breaking change)
- [ ] New feature (non-breaking change)
- [ ] Refactor (code improvement)
- [ ] Breaking change (requires database migration or API contract update)

## Testing Performed
## Checklist
- [ ] I have performed a self-review of my code.
- [ ] My commits follow the Conventional Commits standard.
- [ ] I have updated the documentation (if applicable).
```

### C. Merging Rules
* Squash and Merge: When merging a PR into main, always use "Squash and Merge". This compresses all the minor commits (wip, fixing typo) into a single, clean Conventional Commit on the main branch timeline.
* Delete the feature branch immediately after merging to keep the repository clean.