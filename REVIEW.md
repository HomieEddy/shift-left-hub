---
phase: ui-v3-polish
reviewed: 2026-06-10T00:00:00Z
depth: standard
files_reviewed: 50
files_reviewed_list:
  - frontend/src/app/app.config.ts
  - frontend/src/app/app.html
  - frontend/src/app/app.ts
  - frontend/src/app/core/http/error.interceptor.ts
  - frontend/src/app/core/i18n/translation.service.ts
  - frontend/src/app/core/i18n/translations.ts
  - frontend/src/app/features/admin/kcs-draft-list/kcs-draft-list.component.html
  - frontend/src/app/features/admin/kcs-draft-list/kcs-draft-list.component.ts
  - frontend/src/app/features/admin/llm-settings/llm-settings.component.html
  - frontend/src/app/features/admin/llm-settings/llm-settings.component.ts
  - frontend/src/app/features/admin/user-list/user-list.component.html
  - frontend/src/app/features/admin/user-list/user-list.component.ts
  - frontend/src/app/features/agent/agent-ticket-detail/agent-ticket-detail.component.html
  - frontend/src/app/features/agent/agent-ticket-detail/agent-ticket-detail.component.ts
  - frontend/src/app/features/agent/agent-ticket-list/agent-ticket-list.component.html
  - frontend/src/app/features/agent/agent-ticket-list/agent-ticket-list.component.ts
  - frontend/src/app/features/auth/login/login.component.html
  - frontend/src/app/features/auth/login/login.component.ts
  - frontend/src/app/features/auth/register/register.component.html
  - frontend/src/app/features/auth/register/register.component.ts
  - frontend/src/app/features/chat/chat.component.html
  - frontend/src/app/features/chat/chat.component.ts
  - frontend/src/app/features/kb/admin/article-editor/article-editor.component.html
  - frontend/src/app/features/kb/admin/article-list/article-list.component.html
  - frontend/src/app/features/kb/admin/article-list/article-list.component.ts
  - frontend/src/app/features/kb/admin/tag-manager/tag-manager.component.html
  - frontend/src/app/features/kb/admin/tag-manager/tag-manager.component.ts
  - frontend/src/app/features/kb/public/article-list/article-list.component.html
  - frontend/src/app/features/kb/public/article-search/article-search.component.html
  - frontend/src/app/features/kb/public/article-viewer/article-viewer.component.html
  - frontend/src/app/features/kb/public/article-viewer/article-viewer.component.ts
  - frontend/src/app/features/landing/landing.component.ts
  - frontend/src/app/features/tickets/escalation-form/escalation-form.component.html
  - frontend/src/app/features/tickets/escalation-form/escalation-form.component.ts
  - frontend/src/app/features/tickets/ticket-detail/ticket-detail.component.html
  - frontend/src/app/features/tickets/ticket-detail/ticket-detail.component.ts
  - frontend/src/app/features/tickets/ticket-list/ticket-list.component.html
  - frontend/src/app/features/tickets/ticket-list/ticket-list.component.ts
  - frontend/src/app/shared/ui/badge/badge-utils.ts
  - frontend/src/app/shared/ui/badge/badge.component.ts
  - frontend/src/app/shared/ui/button/button.component.ts
  - frontend/src/app/shared/ui/card/card.component.ts
  - frontend/src/app/shared/ui/confirmation-dialog/confirmation-dialog.component.ts
  - frontend/src/app/shared/ui/modal/modal.component.ts
  - frontend/src/app/shared/ui/pagination/pagination.component.ts
  - frontend/src/app/shared/ui/search-input/search-input.component.ts
  - frontend/src/app/shared/ui/skeleton/skeleton.component.ts
  - frontend/src/app/shared/ui/table/table.component.ts
  - frontend/src/app/shared/ui/toast/toast-container.ts
  - frontend/src/app/shared/ui/toast/toast.model.ts
  - frontend/src/app/shared/ui/toast/toast.service.ts
findings:
  critical: 0
  warning: 5
  info: 9
  total: 14
status: issues_found
---

# Phase ui-v3-polish: Code Review Report

**Reviewed:** 2026-06-10
**Depth:** standard
**Files Reviewed:** 50
**Status:** issues_found — 0 critical, 5 warnings, 9 info items

## Summary

Reviewed 50 frontend source files on the `feat/ui-v3-polish` branch. The branch refactors i18n labels to reactive `computed()` signals, replaces hardcoded Tailwind colors with semantic design tokens, standardizes shared UI components, and applies app-level fixes.

**What's working well:**
- i18n `computed()` signal patterns are correctly applied across most feature components (agent-ticket-detail, agent-ticket-list, escalation-form, user-list, ticket-list)
- Translation service uses `signal()` for `currentLang` with proper `localStorage` persistence
- All RxJS subscriptions use `takeUntilDestroyed()` correctly
- Shared UI components (badge, card, modal, skeleton, table, toast) are proper "dumb" components with `input()`/`output()` only
- `@if`/`@for` control flow used consistently throughout (no legacy `*ngIf`/`*ngFor`)
- Error interceptor properly injects ToastService and TranslationService for i18n error toasts

**Key concerns:**
1. Login and Register components emit **hardcoded English error messages** — existing translation keys are unused
2. Several files retain **hardcoded Tailwind colors** that the branch goal intended to replace with design tokens
3. Debug `console.error`/`console.warn` calls left in production component code
4. Some unused computed labels and dead code (trackByFn, noteError signal) in chat and agent components

---

## Warnings

### WR-01: Login component uses hardcoded error strings — i18n keys exist but are unused

**File:** `frontend/src/app/features/auth/login/login.component.ts:37-39`
**Issue:** Error messages are hardcoded English strings instead of using `translationService.translate()`. The translation keys `error.invalid-credentials` (line 104) and `error.login-failed` (line 105) exist in `translations.ts` but are never used. When a user is viewing the app in French, login failures still show English text.
**Fix:**
```typescript
error: (err: HttpErrorResponse) => {
  this.isLoading = false;
  if (err.status === 401) {
    this.errorMessage = this.translationService.translate('error.invalid-credentials');
  } else {
    this.errorMessage = this.translationService.translate('error.login-failed');
  }
},
```

### WR-02: Register component uses hardcoded error strings — i18n keys exist but are unused

**File:** `frontend/src/app/features/auth/register/register.component.ts:44,62,64`
**Issue:** All three error paths use hardcoded English strings. Translation keys exist: `error.password-rules` (line 108), `error.email-exists` (line 106), `error.registration-failed` (line 107).
**Fix:**
```typescript
if (!this.passwordValid) {
  this.errorMessage = this.translationService.translate('error.password-rules');
  return;
}
// ...
error: (err: HttpErrorResponse) => {
  this.isLoading = false;
  if (err.status === 409) {
    this.errorMessage = this.translationService.translate('error.email-exists');
  } else {
    this.errorMessage = this.translationService.translate('error.registration-failed');
  }
},
```

### WR-03: `console.warn` and `console.error` debug artifacts in production components

**File:** `frontend/src/app/app.ts:58`
**Issue:** `console.warn('KCS pending-count poll failed:', err)` — poll failure is expected/benign; logging to console in production is noise. Consider using a structured logger or suppressing entirely.

**Files:** `frontend/src/app/features/agent/agent-ticket-detail/agent-ticket-detail.component.ts:98,120,139,168`
**Issue:** Four `console.error()` calls in production code (`Failed to load work notes`, `Failed to add work note`, `Failed to claim ticket`, `Failed to resolve ticket`). These are already surfaced to the UI via signal error messages.
**Fix:** Remove `console.error` calls or guard with `isDevMode()`:
```typescript
import { isDevMode } from '@angular/core';
if (isDevMode()) { console.error('Failed to load work notes:', err); }
```

**File:** `frontend/src/app/core/http/error.interceptor.ts:34`
**Issue:** `console.error(\`[HTTP Error ${error.status}]:\`, message)` — logs every HTTP error to console in production. Already surfaced via toast service.
**Fix:** Remove or guard behind `isDevMode()`.

### WR-04: Remaining hardcoded Tailwind `bg-amber-*` colors in escalation form and fallback widget

**File:** `frontend/src/app/features/tickets/escalation-form/escalation-form.component.html:58`
**Issue:** `bg-amber-600 hover:bg-amber-700` — amber tone for escalation submit button. This is the only remaining non-semantic color in an otherwise token-using file.
**Fix:** Map to a design token, e.g., `bg-accent-warning hover:bg-accent-warning/90` or introduce an `bg-accent-escalate` token.

**File:** `frontend/src/app/features/chat/chat.component.html:72-74`
**Issue:** Fallback widget uses `bg-amber-50 border-amber-200 text-amber-800` and `bg-amber-600 hover:bg-amber-700`. Should use semantic tokens consistent with the design system.

### WR-05: SearchInputComponent reads `debounceMs()` at construction time — not reactive

**File:** `frontend/src/app/shared/ui/search-input/search-input.component.ts:53-58`
**Issue:** The `debounceMs` input signal is read once during the constructor. If a parent component passes a dynamic value that changes after construction, the pipeline uses the stale initial value.
**Fix:** If the debounce is expected to be dynamic, recreate the pipeline when the input changes via `effect()`:
```typescript
constructor() {
  effect(() => {
    const ms = this.debounceMs();
    // rebuild searchSubject pipeline with new debounceTime
  });
}
```
If the debounce is always static (current usage suggests it is), document the limitation with a comment on the input.

---

## Info

### IN-01: Chat component defines `trackByFn` method but never references it in template

**File:** `frontend/src/app/features/chat/chat.component.html:19; chat.component.ts:232-234`
**Issue:** Template uses `track $index` (line 19: `@for (msg of messages(); track $index)`) while `trackByFn` (line 232) is defined but unused. Tracking by `$index` causes unnecessary DOM re-creation when message list mutates; tracking by `msg.id` would be more efficient.
**Fix:** Change to `@for (msg of messages(); track msg.id)` and remove the dead `trackByFn` method.

### IN-02: Unused computed labels and signal in AgentTicketDetailComponent

**File:** `frontend/src/app/features/agent/agent-ticket-detail/agent-ticket-detail.component.ts:48,231-233`
**Issue:** 
- `noteError` signal (line 48) is set but never read in the template — the template reads `workNoteError()` instead.
- `addNoteErrorLabel`, `claimErrorLabel`, `resolveErrorLabel` computed signals (lines 231-233) are defined but not referenced in the template.
**Fix:** Remove unused signals to reduce component size and avoid confusion.

### IN-03: ArticleViewerComponent uses getters instead of `computed()` signals for display properties

**File:** `frontend/src/app/features/kb/public/article-viewer/article-viewer.component.ts:52-72`
**Issue:** `displayContent`, `isFallback`, and `fallbackLanguage` are ES6 getters, not `computed()` signals. While they work reactively in templates (because they read `this.translationService.currentLang()` signal internally), the project convention is to use `computed()` for derived reactive state.
**Fix:** Convert to `computed()` signals:
```typescript
displayContent = computed(() => {
  const a = this.article();
  if (a == null) return '';
  return this.translationService.currentLang() === 'fr'
    ? a.contentFr ?? a.contentEn : a.contentEn;
});
```

### IN-04: Hardcoded Tailwind colors in button.component.ts — blocks broad adoption

**File:** `frontend/src/app/shared/ui/button/button.component.ts:39-44,89-94`
**Issue:** Button variant styles use hardcoded `bg-slate-100`, `text-slate-700`, `bg-red-600`, `text-slate-600`, etc. The `danger` variant uses `bg-red-600` instead of `bg-accent-danger`. If this button component is intended to replace all inline buttons, it should use semantic design tokens.
**Fix:** Replace with tokens: `bg-accent-danger` for danger variant, `bg-surface-secondary` → `bg-accent-surface-muted` or similar semantic equivalents.

### IN-05: Hardcoded Tailwind colors in pagination.component.ts

**File:** `frontend/src/app/shared/ui/pagination/pagination.component.ts:26-27`
**Issue:** Page number buttons use `text-slate-600` and `hover:bg-slate-100` instead of `text-text-secondary` and `hover:bg-surface-secondary`.
**Fix:** Replace with design tokens.

### IN-06: Hardcoded Tailwind colors in chat feedback/followup/fallback buttons

**File:** `frontend/src/app/features/chat/chat.component.html:54-55,64-65,95`
**Issue:** Feedback Yes/No and retry buttons use `bg-green-100 text-green-700`, `bg-red-100 text-red-700` instead of semantic tokens like `bg-accent-success-muted text-accent-success` and `bg-accent-danger-muted text-accent-danger`.
**Fix:** Replace with design tokens consistent with the rest of the UI.

### IN-07: Hardcoded Tailwind colors in register password strength meter and rules

**File:** `frontend/src/app/features/auth/register/register.component.html:77-78,85,87,97,99,109,111`
**Issue:** Password strength bar uses `bg-green-500` and `bg-slate-200`. Rule indicators use `text-green-600` and `text-green-500`. Should use `bg-accent-success` / `bg-surface-tertiary` and `text-accent-success`.
**Fix:** Replace with semantic design tokens.

### IN-08: Landing component inline template contains extensive hardcoded Tailwind colors

**File:** `frontend/src/app/features/landing/landing.component.ts:53-480`
**Issue:** The 500-line inline template uses many hardcoded Tailwind utility colors: `bg-slate-900`, `via-slate-800`, `to-slate-950`, `text-slate-300`, `border-slate-600`, `bg-indigo-100`, `text-indigo-600`, `bg-amber-100`, `text-amber-600`, `bg-green-100`, `text-green-600`, `bg-blue-100`, `text-blue-600`, `bg-purple-100`, `text-purple-700`, `bg-cyan-100`, `text-cyan-700`, `text-blue-100`, `bg-emerald-100`, `text-emerald-600`, etc. These should use semantic design tokens or be extracted to CSS custom properties for theming consistency.
**Fix:** Map to design tokens or create landing-specific semantic tokens (e.g., `bg-landing-hero`, `text-landing-accent`).

### IN-09: `app.html` sidebar uses `hover:text-red-500` instead of semantic token

**File:** `frontend/src/app/app.html:134`
**Issue:** Logout button hover state uses `hover:text-red-500` — should be `hover:text-accent-danger` to match the design token system.
**Fix:** Replace `hover:text-red-500` with `hover:text-accent-danger`.

---

_Reviewed: 2026-06-10_
_Reviewer: the agent (gsd-code-reviewer)_
_Depth: standard_
