---
phase: 14-seeding-revamp
plan: 01
subsystem: backend
tags: [java, spring-boot, seeding, data-init, workspace, user]
requires:
  - phase: 12-workspace-management
    provides: WorkspaceService with createWorkspace, assignUserToWorkspace, Workspace entity
  - phase: 01-auth-setup
    provides: User entity, UserRepository, PasswordEncoder, UserRole enum
  - phase: 02-kb-crud
    provides: ArticleRepository with findBySlug
  - phase: 07-ai-integration
    provides: AiConfig entity, AiConfigRepository
provides:
  - MasterSeeder — unified startup seeder creating users, workspaces, memberships, AI config
  - Idempotent seeding pattern (exists-by-unique-field checks)
  - Deletion of old v1.0 DataSeeder, KbSeeder, and 9 IT-themed seed markdown files
affects: [14-seeding-revamp]

tech-stack:
  added: []
  patterns:
    - Single @Order(1) MasterSeeder replaces two separate seeders with different order values
    - Workspace icon set post-creation (createWorkspace does not accept icon param)
    - Department-based role scoping using email convention for user lookup

key-files:
  created:
    - backend/src/main/java/com/shiftleft/hub/common/config/MasterSeeder.java
    - backend/src/main/resources/data/seed/kb/.gitkeep
  modified: []
  deleted:
    - backend/src/main/java/com/shiftleft/hub/common/config/DataSeeder.java
    - backend/src/main/java/com/shiftleft/hub/common/config/KbSeeder.java
    - backend/src/main/resources/data/seed/kb/connect-to-wifi.md
    - backend/src/main/resources/data/seed/kb/laptop-docking-station-setup.md
    - backend/src/main/resources/data/seed/kb/password-policy.md
    - backend/src/main/resources/data/seed/kb/printer-troubleshooting.md
    - backend/src/main/resources/data/seed/kb/remote-access-intranet.md
    - backend/src/main/resources/data/seed/kb/report-security-incident.md
    - backend/src/main/resources/data/seed/kb/reset-vpn-password.md
    - backend/src/main/resources/data/seed/kb/setup-email-on-phone.md
    - backend/src/main/resources/data/seed/kb/software-install-request.md

key-decisions:
  - "MasterSeeder uses workspaceService.findBySlug() for idempotency instead of existsBySlug (delegates to repository)"
  - "Workspace icons set post-creation via workspace.setIcon() since createWorkspace() does not accept icon"
  - "Department users scoped only to their department workspace + Public workspace with MEMBER role"
  - "Admin user gets ADMIN role on all 4 workspaces (auto-assigned by createWorkspace, re-stated explicitly)"
  - "Old article cleanup by known slug constants prevents accidental bulk deletion of user-created articles"

patterns-established:
  - "Single MasterSeeder at @Order(1) replaces multi-seeder pipeline"
  - "Record classes for seed data definitions (UserSeed, WorkspaceSeed)"
  - "Department-email convention (dept.user/tech@company.com) for user identification"

requirements-completed:
  - SEED-01
  - SEED-02
  - SEED-05
  - SEED-06

duration: 2 min
completed: 2026-06-12
---

# Phase 14 Plan 01: Workspace Seeder Revamp Summary

**MasterSeeder.java replaces DataSeeder + KbSeeder — creates 7 users, 4 workspaces, workspace assignments, default AI config, and cleans up old seed articles in one idempotent startup seeder**

## Performance

- **Duration:** 2 min
- **Started:** 2026-06-12T23:37:06Z
- **Completed:** 2026-06-12T23:39:59Z
- **Tasks:** 2
- **Files modified:** 13

## Accomplishments
- Created `MasterSeeder.java` with `@Order(1)` and `@Profile("!test")` — unified seeding entry point
- Seeds 7 users with correct emails and roles (admin, hr.user, hr.tech, legal.user, legal.tech, it.user, it.tech)
- Seeds 4 workspaces (Human Resources, Legal, IT, Public) with correct slugs and icons
- Assigns users with proper role scoping (admin = ADMIN everywhere, department users = MEMBER on dept + Public only)
- Sets `default_workspace_id` to Public workspace for all seed users
- Creates default Ollama AI config (`llama3.2:3b`, `nomic-embed-text`, 0.7 threshold) if none exists
- Deletes old seed articles by known slugs (9 v1.0 IT-themed articles)
- Deleted old `DataSeeder.java`, `KbSeeder.java`, and all 9 old IT-themed markdown files
- All operations fully idempotent via exists-by-unique-field checks

## Task Commits

Each task was committed atomically:

1. **Task 1: Create MasterSeeder.java** — `4cfa38d` (feat)
2. **Task 2: Remove old seeding infrastructure** — `b19219a` (feat)

**Plan metadata:** To be committed as `docs(14-seeding-revamp)` after Summary creation

## Files Created/Modified
- `backend/src/main/java/com/shiftleft/hub/common/config/MasterSeeder.java` — Unified master seed data loader
- `backend/src/main/resources/data/seed/kb/.gitkeep` — Preserves empty directory for future per-workspace subdirectories
- `backend/src/main/java/com/shiftleft/hub/common/config/DataSeeder.java` — DELETED (replaced by MasterSeeder)
- `backend/src/main/java/com/shiftleft/hub/common/config/KbSeeder.java` — DELETED (replaced by per-workspace seeders)
- `backend/src/main/resources/data/seed/kb/*.md` (9 files) — DELETED (old v1.0 IT seed articles)

## Decisions Made
- **Department-based role scoping:** Department users (HR, Legal, IT) only get MEMBER access to their own department workspace + Public. This enforces workspace isolation — HR users cannot see IT workspace content.
- **Icon set post-creation:** `WorkspaceService.createWorkspace()` does not accept an icon parameter, so icons are set via `workspace.setIcon()` + `workspaceRepository.save()` after creation.
- **Old article cleanup safety:** Cleanup targets fixed slug constants only — never bulk-deletes user-created articles. Slugs from v1.0 markdown filenames converted to code conventions (e.g. `connect-to-wifi` → `connect-corporate-wifi`).
- **Seed data records:** Used Java 16+ `record` types (`UserSeed`, `WorkspaceSeed`) for clean, immutable seed data definitions.

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
- **Checkstyle formatting:** MasterSeeder.java initially failed Maven compilation with 9 checkstyle violations (Javadoc `<p>` tag formatting, missing method Javadoc, `{}` placement on record declarations). Fixed code to comply with project checkstyle rules. No logic changes.
- **Git removed empty directory:** `git rm` on all files in `data/seed/kb/` removed the directory itself. Recreated with `.gitkeep` as the plan requires the directory to remain for future per-workspace subdirectories.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- MasterSeeder foundation complete — ready for Plan 2 (per-workspace markdown seed articles with workspace-specific KB seeding)
- Empty `data/seed/kb/` directory ready for per-workspace subdirectories (e.g., `data/seed/kb/it/`, `data/seed/kb/hr/`)
- Old v1.0 seeding infrastructure fully removed — no leftover references

---

*Phase: 14-seeding-revamp*
*Completed: 2026-06-12*

## Self-Check: PASSED

Verification results:
- ✅ MasterSeeder.java exists with @Order(1) and @Profile("!test")
- ✅ Creates 7 users with correct emails/roles
- ✅ Creates 4 workspaces with correct names/slugs
- ✅ Assigns users with correct role scoping
- ✅ Sets default_workspace_id to Public workspace
- ✅ Creates default Ollama AI config if none exists
- ✅ Deletes old seed articles by known slugs
- ✅ All operations idempotent
- ✅ DataSeeder.java deleted
- ✅ KbSeeder.java deleted
- ✅ All 9 old markdown files deleted
- ✅ No remaining Java references to old seeders
- ✅ Maven build compiles without errors (BUILD SUCCESS)
- ✅ SUMMARY.md created in plan directory
