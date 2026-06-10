---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: Workspace Platform
status: planning
last_updated: "2026-06-10T12:05:00.000Z"
progress:
  total_phases: 4
  completed_phases: 0
  total_plans: 4
  completed_plans: 0
  current_phase: 9
  current_focus: "Phase 9 planned — 4 plans in 3 waves ready for execution"
---

# Project State

**Project:** Shift-Left Knowledge Hub
**Initialized:** 2026-05-31
**Status:** 🔷 v2.0 milestone started — planning roadmap

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Shift resolution as close to the user as possible by intercepting Level 0/1 issues before they reach the queue, while simultaneously eliminating the documentation burden on agents.

**Current focus:** Planning v2.0 roadmap — 4 phases (Phases 9-12)

## Milestone Recap

| Milestone | Status |
|-----------|--------|
| v1.0 Initial MVP | ✅ Shipped (8 phases, 60 plans, 41 requirements) |
| v2.0 Workspace Platform | 🔷 Planning roadmap |

## Key Decisions (Carried Forward)

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Modular Monolith | Solo dev, no distributed complexity needed | ✓ Shipped v1.0 |
| PostgreSQL FTS + pgvector | No extra infrastructure needed | ✓ Shipped v1.0 |
| Angular + Spring Boot | Enterprise standard pairing | ✓ Shipped v1.0 |
| JWT with HttpOnly cookies | Security best practice vs localStorage | ✓ Shipped v1.0 |
| Bilingual EN/FR from day one | Rare differentiator, costly to retrofit | ✓ Shipped v1.0 |
| Sequential TKT-NNNN numbering | Human-readable ticket IDs vs raw UUIDs | ✓ Shipped v1.0 |
| JSONB shift_left_context | Flexible schema for AI chat transcript storage | ✓ Shipped v1.0 |

## v2.0 New Decisions

| Decision | Rationale |
|----------|-----------|
| Row-level workspace_id isolation (not schema-per-tenant) | Preserves Flyway, Testcontainers, and Spring AI pgvector integration |
| Hibernate @Filter + AOP for data isolation | Defense-in-depth against cross-tenant leakage |
| Combined DOC + LLM in one phase (Phase 10) | ETL pipeline and LLM config share embedding infrastructure |
| Default Workspace migration for v1.0 data | Backward compatibility without data migration scripts |

## Accumulated Context

- **v1.0 shipped:** 8 phases, 60 plans, 41 requirements
- **Codebase:** ~8,700 frontend LOC + ~5,500 backend main LOC + ~2,200 test LOC
- **Coverage:** 112 backend integration tests + 104 frontend tests + Playwright E2E
- **Current stack:** Angular 21.2, Spring Boot 3.5, PostgreSQL 16 + pgvector, Spring AI
- **v2.0 phases:** 4 phases (9-12), 26 requirements

## Next Steps

1. ✅ Research the domain ecosystem for v2.0 features
2. ✅ Define v2.0 requirements
3. ✅ Create v2.0 roadmap
4. ✅ Plan Phase 9 — 4 plans in 3 waves ready
5. Run `/gsd-execute-phase 9` to execute Phase 9 (Wave 1 → 2 → 3)

---

*Milestone v2.0 — Workspace Platform — Started 2026-06-10*
