---
phase: 01-foundation
plan: 04
type: execute
subsystem: frontend
tags:
  - authentication
  - admin
  - i18n
  - ui
requires:
  - 01-02
  - 01-03
provides:
  - Login UI
  - Register UI with auto-login
  - Admin user management UI
  - Bilingual app shell (EN/FR)
affects:
  - frontend/src/app/app.ts
  - frontend/src/app/app.html
  - frontend/src/app/features/auth/login/login.component.ts
  - frontend/src/app/features/auth/login/login.component.html
  - frontend/src/app/features/auth/register/register.component.ts
  - frontend/src/app/features/auth/register/register.component.html
  - frontend/src/app/features/admin/user-list/user-list.component.ts
  - frontend/src/app/features/admin/user-list/user-list.component.html
tech-stack:
  added:
    - Angular standalone components
    - Signals for component-local state
    - XLIFF 1.2 i18n format
  patterns:
    - inject() over constructor injection
    - FormsModule ngModel for template-driven forms
    - templateUrl external templates
key-files:
  created:
    - frontend/src/app/features/auth/login/login.component.html
    - frontend/src/app/features/auth/register/register.component.html
    - frontend/src/app/features/admin/user-list/user-list.component.html
  modified:
    - frontend/src/app/features/auth/login/login.component.ts
    - frontend/src/app/features/auth/register/register.component.ts
    - frontend/src/app/features/admin/user-list/user-list.component.ts
    - frontend/src/app/app.ts
    - frontend/src/app/app.html
    - frontend/src/locale/messages.xlf
    - frontend/src/locale/messages.fr.xlf
decisions:
  - "App shell component uses existing app.ts/app.html files (not app.component.ts/html as plan referenced)"
  - "Role edit dialog is inline in user-list.component.html (extracted later per YAGNI, scaffolded user-edit-dialog/ directory)"
  - "Language switcher is an EN/FR button toggle with blue active state, inline in header"
  - "Auth nav link and Users route guarded by isAdmin() check per D-27"
duration_minutes: 12
completed_date: "2026-05-31"
commits:
  - cc106c6: "feat(auth): create login and register components with full forms"
  - e2017f3: "feat(admin): create admin user list component with sortable table, role editing, and status toggle"
  - 77150e7: "feat(app): add app shell with language switcher and complete EN/FR i18n translations"
---

# Phase 1 Plan 4: Frontend Auth UI — Summary

**Objective:** Build the full frontend user interface: login/register pages with auto-login flow, admin user management table with role editing and status toggle, app shell with language switcher, and i18n translations (EN/FR).

**One-liner:** Login/register forms with auto-login redirect, sortable admin user table with role edit dialog and status toggle, app header with EN/FR language switcher, and full XLIFF i18n translations.

## Tasks Completed

| # | Task | Type | Status | Commit |
|---|------|------|--------|--------|
| 1 | Create LoginComponent and RegisterComponent with full forms | auto | ✓ Complete | `cc106c6` |
| 2 | Create Admin user list component with table, role editing, and status toggle | auto | ✓ Complete | `e2017f3` |
| 3 | Update AppComponent with header, language switcher, and create i18n translation files | auto | ✓ Complete | `77150e7` |

## Results

### Task 1 — Login and Register Components

- **LoginComponent** (`login.component.ts` + `login.component.html`): Email/password form with ngModel binding, submit handler calls `AuthService.login()`, redirects to `/admin/users` on success, shows error messages for 401 (invalid credentials) and other failures. Loading state disables submit button.
- **RegisterComponent** (`register.component.ts` + `register.component.html`): Display name/email/password form with client-side password validation (8+ chars, 1 uppercase, 1 number), password rule hints shown on focus, auto-login on success per D-18, error handling for 409 (email exists). Redirects to `/admin/users` after registration.
- Both components are standalone, use `inject()` pattern, template-driven forms with `FormsModule`, and external templates.

### Task 2 — Admin User List

- **UserListComponent** (`user-list.component.ts` + `user-list.component.html`): Sortable table with columns: Name, Email, Role (purple/blue badge), Status (green/red dot), Created (formatted date), Actions.
- Clickable column headers toggle sort direction with ▲/▼ indicators.
- "Edit Role" opens modal overlay dialog with Admin/User radio buttons that call `AuthService.updateUserRole()`. "Cancel" closes dialog.
- "Disable"/"Enable" button toggles user status inline via `AuthService.toggleUserStatus()`.
- Loading state ("Loading users..."), error state (red banner), and empty state ("No users found").
- Dialog directory `user-edit-dialog/` scaffolded for future extraction.

### Task 3 — App Shell and i18n

- **App shell** (`app.ts` + `app.html`): Auth-aware header visible only when authenticated. Shows "SL Knowledge Hub" logo, Users nav link (admin only via `isAdmin()`), language switcher toggle (EN/FR with blue active state), user display name, and logout button.
- **messages.xlf**: 27 English trans-units covering login, register, admin, navigation, error messages, and form labels.
- **messages.fr.xlf**: Full French translations for all 27 trans-units.

## Deviations from Plan

### Rule 3 — Plan file mismatch for app component

- **Issue:** Plan referenced `app.component.ts` and `app.component.html`, but the Angular scaffold uses `app.ts` (class `App`) and `app.html`.
- **Fix:** Updated the existing `app.ts` and `app.html` files instead of creating `app.component.ts`/`app.component.html`. This matches the existing scaffold structure and bootstrap import in `main.ts`.
- **Files affected:** `frontend/src/app/app.ts`, `frontend/src/app/app.html`
- **Commit:** `77150e7`

### Plan-specified dialog extraction skipped

- **Issue:** Plan said to skip separate dialog component files and keep dialog inline in user-list.component.html.
- **Fix:** Followed plan exactly — no separate dialog component created. Directory `user-edit-dialog/` exists for future extraction per YAGNI.
- **Commit:** `e2017f3`

## Threat Surface

No new threat surface beyond what `<threat_model>` in PLAN.md already registers:
- T-01-15: All user input uses Angular template binding ([(ngModel)]) — no innerHTML or unsafe DOM APIs ✓
- T-01-16: Admin Users nav link only visible when authService.isAdmin() is true ✓
- T-01-17: Role edit dialog only updates via AuthService.updateUserRole() — backend validates ✓
- T-01-18: Language preference stored in localStorage (accepted risk) ✓

## Verification

- [x] `pnpm build` succeeds (tested after each task)
- [x] Login form renders at /login
- [x] Register form renders at /register
- [x] Register includes displayName field with password rules
- [x] Admin user list loaded at /admin/users with all 6 columns
- [x] Column headers have sort indicators
- [x] Role edit dialog has Admin/User radio options
- [x] Status toggle for enable/disable
- [x] EN/FR language switcher in app header
- [x] Logout button in header
- [x] Auth-aware header visibility
- [x] Admin-only Users nav link

## Self-Check: PASSED

All created/modified files exist and all commits are in git log.

**Files verified:**
- `frontend/src/app/features/auth/login/login.component.ts` ✓
- `frontend/src/app/features/auth/login/login.component.html` ✓
- `frontend/src/app/features/auth/register/register.component.ts` ✓
- `frontend/src/app/features/auth/register/register.component.html` ✓
- `frontend/src/app/features/admin/user-list/user-list.component.ts` ✓
- `frontend/src/app/features/admin/user-list/user-list.component.html` ✓
- `frontend/src/app/app.ts` ✓
- `frontend/src/app/app.html` ✓
- `frontend/src/locale/messages.xlf` ✓
- `frontend/src/locale/messages.fr.xlf` ✓
