---
gsd_state_version: 1.0
milestone: v2.0
milestone_name: Workspace Platform
status: extended
last_updated: "2026-06-13T18:00:00.000Z"
progress:
  total_phases: 8
  completed_phases: 6
  total_plans: 35
  completed_plans: 35
  current_phase: 15
  current_focus: "Phase 15 (File Upload Format Support) planned — 2 plans in 1 wave. Ready to execute."
---

# Project State

**Project:** Shift-Left Knowledge Hub
**Initialized:** 2026-05-31
**Status:** 🔄 v2.0 Extended — Phases 15-16 added (File Upload Format Support + UI Neutralization)

## Project Reference

See: `.planning/PROJECT.md`

**Core value:** Shift resolution as close to the user as possible by intercepting Level 0/1 issues before they reach the queue, while simultaneously eliminating the documentation burden on agents.

**Current focus:** v2.0 Extended — Phases 15-16 added. Phase 15 (File Upload Format Support) pending planning.

## Milestone Recap

| Milestone | Status |
|-----------|--------|
| v1.0 Initial MVP | ✅ Shipped (8 phases, 60 plans, 41 requirements) |
| v2.0 Workspace Platform | ✅ Shipped (6 phases, 35 plans) |

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
- **Coverage:** 112 backend integration tests + 9 backend unit tests (seeders) + 127 frontend tests + Playwright E2E
- **Current stack:** Angular 21.2, Spring Boot 3.5, PostgreSQL 16 + pgvector, Spring AI
- **v2.0 Extended:** 8 phases (9-16), 35 plans executed, 2 phases pending

### Roadmap Evolution

- Phase 15 added: File Upload Format Support (HTML + XML)
- Phase 16 added: UI Neutralization (IT → domain-agnostic)

## Next Steps

1. ✅ v2.0 shipped — Phases 9-14 complete
2. ✅ Phase 15 added: File Upload Format Support
3. ✅ Phase 16 added: UI Neutralization
4. ✅ **Phase 15 planned** — 2 plans in 1 wave
5. 🔲 Execute Phase 15
6. 🔲 Plan + execute Phase 16

---

*Milestone v2.0 — Workspace Platform — Extended with Phases 15-16 on 2026-06-13*
