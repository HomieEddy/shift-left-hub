---
phase: 03-ai-self-service-portal
fixed_at: 2026-06-03T12:00:00Z
review_path: .planning/phases/03-ai-self-service-portal/03-REVIEW.md
iteration: 1
findings_in_scope: 13
fixed: 13
skipped: 0
status: all_fixed
---

# Phase 03: Code Review Fix Report

**Fixed at:** 2026-06-03T12:00:00Z
**Source review:** .planning/phases/03-ai-self-service-portal/03-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 13
- Fixed: 13
- Skipped: 0

## Fixed Issues

### WR-01: Subscription leak — `streamSub` never unsubscribed

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** b97fc06
**Applied fix:** Added `DestroyRef` injection, imported `takeUntilDestroyed` from `@angular/core/rxjs-interop`, added `this.streamSub?.unsubscribe()` before reassignment, and piped the subscription through `takeUntilDestroyed(this.destroyRef)`.

### WR-02: `escalationPayload` typed as `signal<any>` — ban on `any`

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** f5f6c9e
**Applied fix:** Defined `EscalationPayload` interface with `issue`, `transcript`, and `sources` fields. Changed signal type from `signal<any>` to `signal<EscalationPayload | null>`.

### WR-03: Modal has no focus trap or keyboard handling

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`, `frontend/src/app/features/chat/chat.component.html`
**Commit:** 978ffef
**Applied fix:** Added `@HostListener('document:keydown.escape')` to close modal on Escape key. Added `role="dialog"`, `aria-modal="true"`, `aria-labelledby="modal-title"` to overlay. Added `id="modal-title"` to heading. Added `aria-hidden="true"` to decorative emoji. Added `autofocus` to Close button.

### WR-04: `setEscalationPayload` captures first user message, not the latest

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** eaf0b8f
**Applied fix:** Replaced `Array.find()` with filtering for user messages and taking the last element (`[messages.length - 1]`), so the escalation issue captures the most recent user input.

### WR-05: `lastAiMessage` signal is set but never read

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** 7c6ade8
**Applied fix:** Removed the `lastAiMessage` signal declaration and its assignment in the done handler. This eliminates dead state.

### IN-01: `ngAfterViewChecked` triggers scroll on every change detection

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** cf00e52
**Applied fix:** Replaced `ngAfterViewChecked` lifecycle hook (which ran on every CD cycle) with an `effect()` that watches the `messages()` signal, so scroll only triggers when messages actually change.

### IN-02: Modal missing ARIA attributes

**Files modified:** `frontend/src/app/features/chat/chat.component.html`
**Commit:** 978ffef (covered by WR-03)
**Applied fix:** Added `role="dialog"`, `aria-modal="true"`, `aria-labelledby="modal-title"` to overlay div; `aria-hidden="true"` to decorative emoji; `id="modal-title"` to heading.

### IN-03: No `aria-live` region for dynamic chat content

**Files modified:** `frontend/src/app/features/chat/chat.component.html`
**Commit:** 6b0d3c0
**Applied fix:** Added `aria-live="polite"` to the `#chatContainer` div to announce dynamic AI messages.

### IN-04: Hardcoded UI text not wrapped in i18n pipes

**Files modified:** `frontend/src/app/features/chat/chat.component.html`
**Commit:** f717acc
**Applied fix:** Added `i18n` attributes with `@@followUpPrompt`, `@@modalThankYou`, `@@modalSeeYouSoon`, and `@@modalClose` translation IDs to hardcoded text strings.

### IN-05: `trackByFn` uses array index instead of unique identifier

**Files modified:** `frontend/src/app/features/chat/chat.service.ts`, `frontend/src/app/features/chat/chat.component.ts`
**Commit:** 5aa2ce2
**Applied fix:** Added optional `id?: string` field to `ChatMessage` interface. Added incrementing counter to generate unique IDs (`msg-N`) for each user and assistant message. Updated `trackByFn` to return `msg.id` instead of array index.

### IN-06: Empty catch block in `scrollToBottom`

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** 19b74fc
**Applied fix:** Added `console.warn('Scroll failed:', e)` to the previously empty catch block.

### IN-07: `escalationPayload` not reset in `sendMessage()`

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** d295d07
**Applied fix:** Added `this.escalationPayload.set(null)` to the state reset block in `sendMessage()`.

### IN-08: `handleFeedback(false)` constructs hardcoded message for re-send

**Files modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** eaf0b8f (covered by WR-04)
**Applied fix:** Updated to use the last user message instead of the first via `find()`. Added a TODO comment recommending the full conversation transcript be sent as a system instruction in Phase 4.

---

_Fixed: 2026-06-03T12:00:00Z_
_Fixer: the agent (gsd-code-fixer)_
_Iteration: 1_
