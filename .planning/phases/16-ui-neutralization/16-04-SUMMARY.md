---
phase: 16-ui-neutralization
plan: 16-04
status: complete
completed: 2026-06-13
---

# Plan 16-04: Empty States, Error Pages & Onboarding — Summary

## What was built

- **KCS terminology fix** — Fixed remaining "KCS" reference in `kcs.drafts.error.load` ("Failed to load KCS drafts." → "Failed to load drafts.")
- **Final IT terminology sweep** — Zero IT-specific terms remain in user-facing templates or translations
- **Translation deduplication** — Removed 24 duplicate trans-unit IDs from messages.xlf and 7 from messages.fr.xlf (critical build issue)
- **Missing translation keys** — Added 16 missing keys referenced in templates but never defined (EN + FR)
- **CSS fix** — Added missing `--color-accent` CSS variable definition
- **Favicon reference fix** — Removed broken `favicon.ico` reference from index.html
- **Aggressive logout fix** — Changed workspace load error handler from force-logout to error logging
- **Dead code removal** — Removed unused lucide icon imports and unused `currentYear` getter from LandingComponent

## Key decisions

- All empty states were already neutral (pre-existing keys like "No tickets found" use accepted domain language)
- Error pages were already neutral (pre-existing keys like "An unexpected error occurred")
- Remaining KCS references in translation key IDs (e.g., `kcs.drafts.*`) are just internal key names — user-facing text was already neutralized

## Deviations

- None

## Files changed

- `frontend/src/locale/messages.xlf` — Deduplication, missing keys added, KCS error text fix
- `frontend/src/locale/messages.fr.xlf` — Deduplication, missing keys added, KCS error text fix
- `frontend/src/index.html` — Removed broken favicon.ico reference
- `frontend/src/styles.css` — Added `--color-accent` variable
- `frontend/src/app/features/landing/landing.component.ts` — Fixed aggressive logout, removed unused imports and dead code
