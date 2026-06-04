---
phase: 04-escalation-ticketing
fixed_at: 2026-06-04T20:00:00Z
review_path: .planning/phases/04-escalation-ticketing/04-REVIEW.md
iteration: 1
findings_in_scope: 3
fixed: 3
skipped: 0
status: all_fixed
---

# Phase 4: Code Review Fix Report

**Fixed at:** 2026-06-04T20:00:00Z
**Source review:** .planning/phases/04-escalation-ticketing/04-REVIEW.md
**Iteration:** 1

**Summary:**
- Findings in scope: 3
- Fixed: 3
- Skipped: 0

## Fixed Issues

### WR-01: Race condition in ticket number generation

**Files modified:** `backend/src/main/java/com/shiftleft/hub/ticket/domain/TicketNumberSequenceRepository.java`, `backend/src/main/java/com/shiftleft/hub/ticket/service/TicketService.java`
**Commit:** `f91024b`
**Applied fix:** Added `@Lock(LockModeType.PESSIMISTIC_WRITE)` query method `findByIdWithLock()` to `TicketNumberSequenceRepository` with a JPQL query that acquires a pessimistic write lock on the sequence row. Updated `TicketService.generateTicketNumber()` to use `findByIdWithLock(1L)` instead of `findById(1L)`, eliminating the race window between reading the current sequence number and writing the incremented value back.

### WR-02: Missing `UsernameNotFoundException` handler in GlobalExceptionHandler

**File modified:** `backend/src/main/java/com/shiftleft/hub/common/config/GlobalExceptionHandler.java`
**Commit:** `68decb4`
**Applied fix:** Added `import org.springframework.security.core.userdetails.UsernameNotFoundException` and a dedicated `@ExceptionHandler(UsernameNotFoundException.class)` method returning HTTP 404 (Not Found). Previously, this exception fell through to the generic `Exception` handler and returned HTTP 500.

### WR-03: Direct mutation of objects held inside Angular signal

**File modified:** `frontend/src/app/features/chat/chat.component.ts`
**Commit:** `7d9277e`
**Applied fix:** Replaced the mutable `assistantMsg.content += event.content` pattern in the streaming token handler with an immutable signal update. The new code creates a shallow copy of the messages array and replaces the last message object with a new object reference containing the accumulated content, ensuring Angular's signal change detection reliably picks up the update.

---

## Skipped Issues

None — all 3 in-scope findings were successfully fixed.

---

_Fixed: 2026-06-04T20:00:00Z_
_Fixer: the agent (gsd-code-fixer)_
_Iteration: 1_
