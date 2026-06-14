---
phase: 16-ui-neutralization
plan: 16-03
status: complete
completed: 2026-06-13
---

# Plan 16-03: Unified Dashboard — Summary

## What was built

- **Unified dashboard** — Replaced role-branched signed-in section (admin/agent/user branches) with a single unified layout
- **Workspace context header** — Shows current workspace name, slug, icon, and user display name
- **Quick action cards** — Knowledge Base, AI Assistant, My Requests (visible to all authenticated users)
- **Role-adaptive sections** — Admin section shows Workspace Settings link; Agent/Admin section shows Queue link — within unified layout
- **Recent Activity** — Placeholder section with "No recent activity" message
- **Dashboard translation keys** — Added `dashboard.quick-actions`, `dashboard.recent-activity`, `dashboard.recent-empty`, `nav.workspace-settings`, `dashboard.guest.title`, `dashboard.guest.desc` (EN + FR)

## Key decisions

- Unified dashboard follows Linear/Notion-style workspace home pattern
- Quick actions are the primary interface (3 cards: KB, AI Assistant, My Requests)
- Role-adaptive sections are secondary, within the unified layout
- Gradient backgrounds use the new warm slate/charcoal palette

## Deviations

- None

## Files changed

- `frontend/src/app/features/landing/landing.component.html` — Complete replace of signed-in section (admin/agent/user branches removed, unified dashboard added)
- `frontend/src/locale/messages.xlf` — Added dashboard translation keys
- `frontend/src/locale/messages.fr.xlf` — Added French dashboard translations
