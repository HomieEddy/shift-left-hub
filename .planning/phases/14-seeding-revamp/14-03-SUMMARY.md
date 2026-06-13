---
phase: 14-seeding-revamp
plan: 03
subsystem: seed
tags:
  - seeding
  - workspace
  - it
  - public
  - markdown
requires:
  - 14-01
  - 14-02
provides:
  - IT workspace seed data (12 tags, 10 articles)
  - Public workspace seed data (6 tags, 10 articles)
affects:
  - backend/src/main/java/com/shiftleft/hub/common/config/ItSeeder.java
  - backend/src/main/java/com/shiftleft/hub/common/config/PublicSeeder.java
  - backend/src/main/resources/data/seed/kb/it/*.md
  - backend/src/main/resources/data/seed/kb/public/*.md
tech-stack:
  added:
    - PathMatchingResourcePatternResolver (classpath resource scanning)
    - Lombok @Builder + workspaceId setter pattern
  patterns:
    - Per-workspace seeder class with @Order(2), @Profile("!test")
    - Frontmatter + bilingual body parsing from markdown
    - Idempotency via slug-based article check and name_en+workspaceId tag check
key-files:
  created:
    - backend/src/main/java/com/shiftleft/hub/common/config/ItSeeder.java
    - backend/src/main/java/com/shiftleft/hub/common/config/PublicSeeder.java
    - backend/src/main/resources/data/seed/kb/it/it-vpn-setup.md
    - backend/src/main/resources/data/seed/kb/it/it-password-policy.md
    - backend/src/main/resources/data/seed/kb/it/it-account-creation.md
    - backend/src/main/resources/data/seed/kb/it/it-email-setup.md
    - backend/src/main/resources/data/seed/kb/it/it-wifi-access.md
    - backend/src/main/resources/data/seed/kb/it/it-printing-guide.md
    - backend/src/main/resources/data/seed/kb/it/it-remote-access.md
    - backend/src/main/resources/data/seed/kb/it/it-software-installation.md
    - backend/src/main/resources/data/seed/kb/it/it-hardware-requests.md
    - backend/src/main/resources/data/seed/kb/it/it-security-incident-reporting.md
    - backend/src/main/resources/data/seed/kb/public/public-company-overview.md
    - backend/src/main/resources/data/seed/kb/public/public-getting-started.md
    - backend/src/main/resources/data/seed/kb/public/public-general-faq.md
    - backend/src/main/resources/data/seed/kb/public/public-announcements.md
    - backend/src/main/resources/data/seed/kb/public/public-support-contacts.md
    - backend/src/main/resources/data/seed/kb/public/public-workplace-policies.md
    - backend/src/main/resources/data/seed/kb/public/public-it-helpdesk-basics.md
    - backend/src/main/resources/data/seed/kb/public/public-hr-overview.md
    - backend/src/main/resources/data/seed/kb/public/public-office-locations.md
    - backend/src/main/resources/data/seed/kb/public/public-company-directory.md
decisions:
  - Tags use setWorkspaceId() post-build instead of builder method (parent class field from WorkspaceAwareEntity not in @Builder)
metrics:
  duration: ~25 min
  completed_date: 2026-06-12
---

# Phase 14 Plan 03: IT & Public Workspace Seed Content Summary

**One-liner:** Created IT and Public workspace seed content — 20 bilingual markdown files with 18 workspace-specific tags and 20 published articles managed by two independent `@Order(2)` seeders.

## Tasks Completed

### Task 1: Create IT workspace seed content (10 markdown files + ItSeeder.java)

**Commit:** `9bf3bc7`

Created 10 bilingual IT markdown files covering VPN setup, password policy, account creation, email setup, Wi-Fi access, printing guide, remote access, software installation, hardware requests, and security incident reporting. Created `ItSeeder.java` at `@Order(2)` with 12 IT tags (preserving old KbSeeder color palette per D-12, excluding 'mobile' and 'it-requests') and 10 published articles in the IT workspace authored by admin.

### Task 2: Create Public workspace seed content (10 markdown files + PublicSeeder.java)

**Commit:** `c4c819e`

Created 10 bilingual Public markdown files covering company overview, getting started guide, FAQ, announcements, support contacts, workplace policies, IT helpdesk basics, HR overview, office locations, and company directory. Created `PublicSeeder.java` at `@Order(2)` with 6 Public tags (General, Announcements, FAQ, Getting Started, Support, Policies per D-13/D-14) and 10 published articles in the Public workspace authored by admin.

## Deviations from Plan

### Rule 3 — Auto-fixed Blocking Issues

**1. [Rule 3 — Blocking] Fixed workspaceId builder invocation in all per-workspace seeders**
- **Found during:** Maven compilation
- **Issue:** `@Builder` on Article/Tag entities does not include parent class fields from `WorkspaceAwareEntity`. The `.workspaceId(UUID)` method does not exist on the builder.
- **Fix:** Replaced `.workspaceId(...)` in builder chains with post-build `.setWorkspaceId(...)` calls in ItSeeder, PublicSeeder, and also in HrSeeder and LegalSeeder (Plan 14-02) which had the same issue blocking the full build.
- **Files modified:** `ItSeeder.java`, `PublicSeeder.java`, `HrSeeder.java`, `LegalSeeder.java`
- **Commit:** `d62fd5d`

**2. [Rule 3 — Style] Fixed checkstyle violations**
- **Issues:** Missing Javadoc on `seed()` methods in ItSeeder/PublicSeeder, and single-line `if` without `{}` (NeedBraces rule)
- **Fix:** Added Javadoc to both `seed()` methods; added braces to the `if (filename == null) continue;` statements
- **Commit:** `d62fd5d`

## Verification Results

| Check | Status |
|-------|--------|
| 10 IT markdown files exist | ✅ Passed |
| 10 Public markdown files exist | ✅ Passed |
| ItSeeder.java with @Order(2) | ✅ Passed |
| PublicSeeder.java with @Order(2) | ✅ Passed |
| All files have valid frontmatter (slug, title_en, title_fr, tags, excerpt, excerpt_fr) | ✅ Passed |
| All files have bilingual body with `<!-- FR -->` separator | ✅ Passed |
| Maven compile passes without errors | ✅ Passed |
| No untracked files left | ✅ Passed |

## Key Metrics

- **22 files created** (2 Java seeders + 20 markdown files)
- **4 files modified** (fixes across all 4 per-workspace seeders)
- **3 commits** (2 feature + 1 fix)
- **~3009 lines added** across all new files

## Self-Check: PASSED
