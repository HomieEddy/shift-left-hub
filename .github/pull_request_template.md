name: Pull Request
description: Default pull request template for backend and frontend changes.
body:
  - type: input
    id: title
    attributes:
      label: Title
      description: '`type(scope): short description` — e.g. `fix(frontend): tier 15 — 4 real bugs`'
    validations:
      required: true
  - type: textarea
    id: summary
    attributes:
      label: What this PR does
      placeholder: '1-2 sentences. List any user-facing changes.'
    validations:
      required: true
  - type: textarea
    id: requirements
    attributes:
      label: Requirements
      placeholder: 'REQ-IDs or phase numbers from .planning/REQUIREMENTS.md (or ROADMAP.md).'
    validations:
      required: false
  - type: textarea
    id: decisions
    attributes:
      label: Decisions
      placeholder: Any non-obvious decisions, trade-offs, or scope cuts. Skip if trivial.
    validations:
      required: false
  - type: textarea
    id: testing
    attributes:
      label: Test counts
      placeholder: 'e.g. Backend: 469 (was 459, +10). Frontend: 306 (unchanged).'
    validations:
      required: true
  - type: checkboxes
    id: checklist
    attributes:
      label: Checklist
      options:
        - label: '`mvn verify` (backend) or `pnpm lint && pnpm test` (frontend) passes locally'
          required: true
        - label: I self-reviewed the diff before requesting review
          required: true
        - label: No secrets, API keys, or `.env` files in the diff
          required: true
        - label: No `.planning/` files in the diff (those are gitignored)
          required: true
