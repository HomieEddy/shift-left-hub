---
phase: 03-ai-self-service-portal
reviewed: 2026-06-03T12:00:00Z
depth: standard
files_reviewed: 2
files_reviewed_list:
  - frontend/src/app/features/chat/chat.component.ts
  - frontend/src/app/features/chat/chat.component.html
findings:
  critical: 0
  warning: 5
  info: 8
  total: 13
status: issues_found
---

# Phase 03: Code Review Report

**Reviewed:** 2026-06-03T12:00:00Z
**Depth:** standard
**Files Reviewed:** 2
**Status:** issues_found

## Summary

Reviewed the AI Self-Service Portal chat component (`.ts` + `.html`) that adds a follow-up flow after the AI feedback loop. The overall architecture is sound — signals are reset consistently in `sendMessage()`, the follow-up state machine (feedback → follow-up → close modal) follows a clear linear progression, and the template uses Angular structural directives properly.

Five warnings and eight info items were identified. Key concerns include: a subscription leak that contradicts the project's `takeUntilDestroyed()` guideline, a missing focus trap on the modal (accessibility), dead signal state (`lastAiMessage`), weak typing (`signal<any>`), and a subtle bug in escalation payload extraction that uses the first user message instead of the latest in multi-turn conversations. No critical security issues were found — ngx-markdown sanitizes by default, and all template interpolations use safe text bindings.

## Warnings

### WR-01: Subscription leak — `streamSub` never unsubscribed

**File:** `frontend/src/app/features/chat/chat.component.ts:30,63,84,88`
**Issue:** The `streamSub` subscription is assigned on every `sendMessage()` call but never unsubscribed. The component lacks an `OnDestroy` lifecycle hook. This violates the project's Angular guideline (AGENTS.md): *"If `.subscribe()` is unavoidable in TS, use `takeUntilDestroyed()` (Angular 16+)."* Each call to `sendMessage()` without cleanup of the prior subscription creates a leak window — the old subscription's `complete`/`error` handlers may fire after a new stream begins.

**Fix:**
```typescript
import { Component, ..., DestroyRef } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

// In class:
private destroyRef = inject(DestroyRef);

// In sendMessage():
this.streamSub = events.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ ... });
```

Alternatively, call `this.streamSub?.unsubscribe()` before reassigning on line 63. Given the project Angular 21 target and the AGENTS.md guideline, prefer `takeUntilDestroyed()`.

### WR-02: `escalationPayload` typed as `signal<any>` — ban on `any`

**File:** `frontend/src/app/features/chat/chat.component.ts:27`
**Issue:** The project explicitly bans `any` in AGENTS.md: *"Strong Typing — Ban `any`. Define Interfaces/Types for all payloads. Use `unknown` when shape is uncertain (forces type-check before access)."* The `escalationPayload` signal is typed `signal<any>`, bypassing type safety.

**Fix:**
```typescript
interface EscalationPayload {
  issue: string;
  transcript: ChatMessage[];
  sources: { articleId: string; title: string; slug: string; score: number }[];
}

escalationPayload = signal<EscalationPayload | null>(null);
```

### WR-03: Modal has no focus trap or keyboard handling

**File:**
- `frontend/src/app/features/chat/chat.component.ts:122-124`
- `frontend/src/app/features/chat/chat.component.html:65-74`

**Issue:** The close modal overlay has no focus management. When the modal opens:
1. Focus is not moved into the modal — Tab navigation can reach elements behind the overlay
2. The Escape key does not close the modal
3. When the modal closes, focus is not restored to the trigger element

This creates a poor experience for keyboard-only and screen-reader users.

**Fix:** Add focus trap and Escape key handler:

```typescript
// In component:
@HostListener('document:keydown.escape')
onEscape() {
  if (this.showCloseModal()) {
    this.closeModal();
  }
}
```

```html
<!-- Template modal wrapper -->
<div *ngIf="showCloseModal()"
     role="dialog"
     aria-modal="true"
     aria-labelledby="modal-title"
     class="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
  <div #modalContent class="bg-white rounded-2xl shadow-xl px-8 py-10 text-center max-w-sm mx-4">
    <div class="text-5xl mb-4" aria-hidden="true">👋</div>
    <h2 id="modal-title" class="text-xl font-semibold text-slate-800 mb-2">Thank You</h2>
    <p class="text-slate-500 mb-6">See you soon!</p>
    <button (click)="closeModal()" class="px-6 py-2 bg-blue-600 text-white ..." autofocus>
      Close
    </button>
  </div>
</div>
```

For a production-grade focus trap, consider `@angular/cdk/a11y`'s `FocusTrap` directive.

### WR-04: `setEscalationPayload` captures first user message, not the latest

**File:** `frontend/src/app/features/chat/chat.component.ts:96`
**Issue:** `this.messages().find(m => m.role === 'user')?.content` uses `Array.find()`, which returns the **first** matching element. In a multi-turn conversation, this captures the initial user query as the "issue" rather than the most recent user input. This means escalation context is always the first message, not what the user just said.

**Fix:**
```typescript
// Use the last user message for escalation context
const userMessages = this.messages().filter(m => m.role === 'user');
const lastUserContent = userMessages.length > 0
  ? userMessages[userMessages.length - 1].content
  : '';
```

### WR-05: `lastAiMessage` signal is set but never read

**File:** `frontend/src/app/features/chat/chat.component.ts:25,70`
**Issue:** `lastAiMessage` is declared as a signal (line 25) and set when the AI response completes (line 70), but it is never read anywhere — not in the template, not in computed signals, not method calls. This is either dead code or an incomplete feature hook that should be removed or wired up.

**Fix:** Remove the signal and its assignment if unused:
```typescript
// Remove line 25
// Remove line 70: this.lastAiMessage.set(assistantMsg.content);
```

If intended for future use (e.g., logging, analytics), add a comment explaining the purpose and add a read-site trace.

## Info

### IN-01: `ngAfterViewChecked` triggers scroll on every change detection

**File:** `frontend/src/app/features/chat/chat.component.ts:33-35`
**Issue:** `ngAfterViewChecked` fires after every change detection cycle — including keystrokes in the input field, resizing, etc. Each call recalculates layout via `scrollToBottom()`. While the try-catch swallows errors, this is an unnecessary performance tax in a chat component that could accumulate many messages over a session.

**Fix:** Trigger scroll only when messages change (consider `effect()` watching the messages signal):
```typescript
private scrollEffect = effect(() => {
  this.messages(); // subscribe to changes
  this.scrollToBottom();
});
```

### IN-02: Modal missing ARIA attributes

**File:** `frontend/src/app/features/chat/chat.component.html:65-74`
**Issue:** The close modal overlay lacks accessible semantics: no `role="dialog"`, `aria-modal="true"`, `aria-labelledby`, or `aria-describedby`. Screen readers will not identify it as a dialog.

**Fix:** Add the attributes as shown in WR-03's fix snippet. Reference a heading element with `aria-labelledby` for the dialog title.

### IN-03: No `aria-live` region for dynamic chat content

**File:** `frontend/src/app/features/chat/chat.component.html:7`
**Issue:** The chat container (line 7) has no `aria-live` attribute. Screen readers will not announce new AI messages, feedback prompts, or follow-up prompts as they appear.

**Fix:**
```html
<div #chatContainer aria-live="polite" class="flex-1 overflow-y-auto px-4 py-6 space-y-4">
```

### IN-04: Hardcoded UI text not wrapped in i18n pipes

**File:** `frontend/src/app/features/chat/chat.component.html:39,68-69`
**Issue:** AGENTS.md specifies bilingual i18n (EN/FR) from Phase 1. The new follow-up text ("Is there anything else I can help you with?") and modal text ("Thank You", "See you soon!") are hardcoded English strings. These will need i18n wrapping before production.

**Fix:** Use Angular's `@angular/localize` `$localize` tag or a translation pipe:
```html
<span i18n="@@followUpPrompt">Is there anything else I can help you with?</span>
```

### IN-05: `trackByFn` uses array index instead of unique identifier

**File:** `frontend/src/app/features/chat/chat.component.ts:146-148`
**Issue:** `trackByFn` returns the array index, which works for append-only lists but can cause DOM recycling issues if messages are reordered or filtered. Using a unique message ID is more robust.

**Fix:** Add an `id` field to `ChatMessage` and track by `msg.id`.

### IN-06: Empty catch block in `scrollToBottom`

**File:** `frontend/src/app/features/chat/chat.component.ts:143`
**Issue:** The `catch` block at line 143 is empty (`catch { }`), silently swallowing any errors from the `scrollTo` call. While scroll errors are typically benign, empty catch blocks hide bugs during development.

**Fix:** Either remove the try-catch (if the operation truly can't fail) or log the error:
```typescript
catch (e) {
  console.warn('Scroll failed:', e);
}
```

### IN-07: `escalationPayload` not reset in `sendMessage()`

**File:** `frontend/src/app/features/chat/chat.component.ts:42-46`
**Issue:** All state signals are reset in `sendMessage()` except `escalationPayload`. If a user sends a new message after a previous escalation, the old payload persists. This could cause stale escalation data to be submitted in Phase 4.

**Fix:** Add `this.escalationPayload.set(null);` to the reset block at line 46.

### IN-08: `handleFeedback(false)` constructs hardcoded message for re-send

**File:** `frontend/src/app/features/chat/chat.component.ts:108`
**Issue:** When the user gives negative feedback, the component constructs a message:
```
"The user indicated this did not solve their problem. Original issue: " + followUp
```
This hardcoded template may confuse the AI in multi-turn conversations (where multiple user messages exist). It also uses the first user message (`find`) rather than the conversation context, compounding WR-04's issue.

**Fix:** Consider sending the full conversation transcript with an embedded system instruction instead of a fabricated user message. Alternatively, append a system-level context note rather than injecting it as a user message:
```typescript
// When wiring up AI context, include this as a system instruction, not as a user message
```

---

_Reviewed: 2026-06-03T12:00:00Z_
_Reviewer: gsd-code-reviewer (standard depth)_
_Depth: standard_
