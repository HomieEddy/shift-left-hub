---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: Workspace Platform
status: planning
last_updated: "2026-06-12T21:42:00.000Z"
progress:
  total_phases: 6
  completed_phases: 5
  total_plans: 32
  completed_plans: 29
  current_phase: 14
  current_focus: "Phase 14 planned — 3 plans in 1 wave. Master seeder + seed content for 4 workspace domains (HR, Legal, IT, Public)."
---

# Project State

**Project:** Shift-Left Knowledge Hub
**Initialized:** 2026-05-31
**Status:** ✅ v2.0 milestone complete (with Frontend Cleanup polish)

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Shift resolution as close to the user as possible by intercepting Level 0/1 issues before they reach the queue, while simultaneously eliminating the documentation burden on agents.

**Current focus:** v2.0 shipped — all 5 phases (9-13) complete, 29/29 plans executed, 127 frontend tests pass.

## Milestone Recap

| Milestone | Status |
|-----------|--------|
| v1.0 Initial MVP | ✅ Shipped (8 phases, 60 plans, 41 requirements) |
| v2.0 Workspace Platform | ✅ Shipped (5 phases, 29 plans) |

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
| **Phase 13: Extract all inline templates** | Angular Style Guide compliance, improves maintainability and diffs |
| **Phase 14: Master + per-workspace seeders** | One master (users/workspaces/roles) + four per-workspace seeders (tags/articles) |
| **Phase 14: Public workspace is default** | New users' `default_workspace_id` points to Public workspace |
| **Phase 14: 7 seed users with workspace-scoped roles** | Admin has ADMIN on all 4 workspaces; dept users have MEMBER on dept + Public |

## Accumulated Context

- **v1.0 shipped:** 8 phases, 60 plans, 41 requirements
- **Codebase:** ~8,700 frontend LOC + ~5,500 backend main LOC + ~2,200 test LOC
- **Coverage:** 112 backend integration tests + 127 frontend tests + Playwright E2E
- **Current stack:** Angular 21.2, Spring Boot 3.5, PostgreSQL 16 + pgvector, Spring AI
- **v2.0 phases (extended):** 5 phases (9-13), 29 plans

## Next Steps

1. ✅ Research the domain ecosystem for v2.0 features
2. ✅ Define v2.0 requirements
3. ✅ Create v2.0 roadmap
4. ✅ Plan Phase 9 — 4 plans in 3 waves ready
5. ✅ Plan Phase 10 — 4 plans in 3 waves ready
6. ✅ Execute Phase 9 — Workspace Foundation
7. ✅ Execute Phase 10 — Document Ingestion + BYO LLM
8. ✅ Plan Phase 11 — Domain-Agnostic AI (6 plans in 4 waves)
9. ✅ Execute Phase 11 — Domain-Agnostic AI complete
10. ✅ Run `/gsd-plan-phase 12` — Phase 12 planned (5 plans in 2 waves)
11. ✅ Execute Phase 12 — Workspace Management UI (5 plans, 2 waves)
12. ✅ Add Phases 13 to roadmap (Frontend Cleanup)
13. ✅ Plan Phase 13 — 4 plans in 2 waves
14. ✅ Execute Phase 13 — Frontend Cleanup complete (4 plans, 2 waves)
  15. ✅ Plan Phase 14 — Seeding Revamp (3 plans in 1 wave)
  16. 🔲 Next: `/gsd-execute-phase 14` — Execute Phase 14

---

*Milestone v2.0 — Workspace Platform — Extended with Phase 13 on 2026-06-12*
