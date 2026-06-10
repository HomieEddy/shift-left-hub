---
phase: ui-v3-polish
fixed_at: 2026-06-10T00:00:00Z
review_path: REVIEW.md
iteration: 1
findings_in_scope: 14
fixed: 14
skipped: 0
status: all_fixed
---

# Phase ui-v3-polish: Code Review Fix Report

**Fixed at:** 2026-06-10
**Source review:** REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 14
- Fixed: 14
- Skipped: 0

## Fixed Issues

### WR-01: Login component uses hardcoded error strings

**Files modified:** `frontend/src/app/features/auth/login/login.component.ts`
**Commit:** 9458de1
**Applied fix:** Replaced hardcoded English error messages with `translationService.translate()` calls using i18n keys `error.invalid-credentials` and `error.login-failed`.

### WR-02: Register component uses hardcoded error strings

**Files modified:** `frontend/src/app/features/auth/register/register.component.ts`
**Commit:** 4204a88
**Applied fix:** Replaced three hardcoded English error messages with `translationService.translate()` calls using i18n keys `error.password-rules`, `error.email-exists`, and `error.registration-failed`.

### WR-03: console.warn and console.error debug artifacts in production

**Files modified:** `frontend/src/app/app.ts`, `frontend/src/app/features/agent/agent-ticket-detail/agent-ticket-detail.component.ts`, `frontend/src/app/core/http/error.interceptor.ts`
**Commit:** 27cd288
**Applied fix:** Guarded all `console.warn` and `console.error` calls behind `isDevMode()` check (imported from `@angular/core`). Affected: 1 console.warn in app.ts, 4 console.error in agent-ticket-detail, 1 console.error in error interceptor.

### WR-04: Remaining hardcoded Tailwind bg-amber-* colors

**Files modified:** `frontend/src/app/features/tickets/escalation-form/escalation-form.component.html`, `frontend/src/app/features/chat/chat.component.html`
**Commit:** 8ba5f52
**Applied fix:** Replaced `bg-amber-600 hover:bg-amber-700` with `bg-accent-warning hover:bg-accent-warning/90` in escalation form button. Replaced fallback widget colors: `bg-amber-50` → `bg-accent-warning-muted`, `border-amber-200` → `border-accent-warning`, `text-amber-800` → `text-accent-warning`, and button `bg-amber-600 hover:bg-amber-700` → `bg-accent-warning hover:bg-accent-warning/90`.

### WR-05: SearchInputComponent reads debounceMs() at construction time

**Files modified:** `frontend/src/app/shared/ui/search-input/search-input.component.ts`
**Commit:** 4587760
**Applied fix:** Added JSDoc comment on `debounceMs` input signal documenting that it is read once at construction time and dynamic changes after init are not reflected. Current usage is static, so this is documentation-only.

### IN-01: Chat component defines trackByFn but never uses it

**Files modified:** `frontend/src/app/features/chat/chat.component.html`, `frontend/src/app/features/chat/chat.component.ts`
**Commit:** e0c6234
**Applied fix:** Changed template `@for` tracking from `track $index` to `track msg.id`. Removed the unused `trackByFn` method from the component class.

### IN-02: Unused computed labels and signal in AgentTicketDetailComponent

**Files modified:** `frontend/src/app/features/agent/agent-ticket-detail/agent-ticket-detail.component.ts`
**Commit:** 182a496
**Applied fix:** Removed unused `noteError` signal (its single usage redirected to `workNoteError` which the template actually reads). Removed three unused computed signals: `addNoteErrorLabel`, `claimErrorLabel`, `resolveErrorLabel`.

### IN-03: ArticleViewerComponent uses getters instead of computed() signals

**Files modified:** `frontend/src/app/features/kb/public/article-viewer/article-viewer.component.ts`, `frontend/src/app/features/kb/public/article-viewer/article-viewer.component.html`
**Commit:** 08c26d3
**Applied fix:** Converted `displayContent`, `isFallback`, and `fallbackLanguage` from ES6 getters to `computed()` signals. Updated template references to use signal invocation syntax (`isFallback()`, `fallbackLanguage()`, `displayContent()`).

### IN-04: Hardcoded Tailwind colors in button.component.ts

**Files modified:** `frontend/src/app/shared/ui/button/button.component.ts`
**Commit:** b184d1e
**Applied fix:** Replaced hardcoded Tailwind colors with semantic design tokens in both `ButtonComponent` and `IconButtonComponent` variant maps:
- `secondary`: `bg-slate-100 text-slate-700` → `bg-surface-tertiary text-text-primary`
- `outline`: `border-slate-300 text-slate-700 hover:bg-slate-50` → `border-border-default text-text-primary hover:bg-surface-secondary`
- `ghost`: `text-slate-600 hover:bg-slate-100` → `text-text-secondary hover:bg-surface-tertiary`
- `danger`: `bg-red-600 hover:bg-red-700` → `bg-accent-danger hover:bg-accent-danger/90`

### IN-05: Hardcoded Tailwind colors in pagination.component.ts

**Files modified:** `frontend/src/app/shared/ui/pagination/pagination.component.ts`
**Commit:** 4c78f32
**Applied fix:** Replaced `text-slate-600` with `text-text-secondary` and `hover:bg-slate-100` with `hover:bg-surface-secondary` for page number buttons.

### IN-06: Hardcoded Tailwind colors in chat feedback/followup buttons

**Files modified:** `frontend/src/app/features/chat/chat.component.html`
**Commit:** 1a1bd9d
**Applied fix:** Replaced green/red button colors with semantic tokens across all 5 buttons:
- `bg-green-100 text-green-700 hover:bg-green-200` → `bg-accent-success-muted text-accent-success hover:bg-accent-success-muted/80` (Yes buttons)
- `bg-red-100 text-red-700 hover:bg-red-200` → `bg-accent-danger-muted text-accent-danger hover:bg-accent-danger-muted/80` (No/Retry buttons)

### IN-07: Hardcoded Tailwind colors in register password strength meter

**Files modified:** `frontend/src/app/features/auth/register/register.component.html`
**Commit:** 8cbbf82
**Applied fix:** Replaced password strength and rule indicator colors:
- `bg-green-500` → `bg-accent-success`, `bg-slate-200` → `bg-surface-tertiary`
- `text-green-600` → `text-accent-success` (rule list items)
- `text-green-500` → `text-accent-success` (check icons)

### IN-08: Landing component inline template extensive hardcoded colors

**Files modified:** `frontend/src/app/features/landing/landing.component.ts`
**Commit:** a48d0a5
**Applied fix:** Replaced hardcoded Tailwind accent colors with semantic design tokens across the inline template (~48 replacements):
- `bg-indigo-100` → `bg-accent-info-muted`, `text-indigo-600/700` → `text-accent-info`
- `bg-amber-100` → `bg-accent-warning-muted`, `text-amber-600/700` → `text-accent-warning`
- `bg-green-100` → `bg-accent-success-muted`, `text-green-600` → `text-accent-success`
- `bg-blue-100` → `bg-accent-info-muted`, `text-blue-600` → `text-accent-info`
- `bg-emerald-100` → `bg-accent-success-muted`, `text-emerald-500/600` → `text-accent-success`
- `bg-purple-100` → `bg-accent-info-muted`, `text-purple-700` → `text-accent-info`
- `bg-cyan-100` → `bg-accent-info-muted`, `text-cyan-700` → `text-accent-info`
- `text-blue-100` → `text-white`, `hover:bg-blue-50` → `hover:bg-surface-secondary`
- `hover:border-indigo-200` → `hover:border-accent-info/50`
- `hover:border-amber-200` → `hover:border-accent-warning/50`
- `hover:border-emerald-200` → `hover:border-accent-success/50`
- Kept gradient backgrounds and dark hero section colors (intentional design choices)

### IN-09: app.html sidebar hover:text-red-500

**Files modified:** `frontend/src/app/app.html`
**Commit:** 1ab5076
**Applied fix:** Replaced `hover:text-red-500` with `hover:text-accent-danger` on the logout button.

---

_Fixed: 2026-06-10_
_Fixer: the agent (gsd-code-fixer)_
_Iteration: 1_
