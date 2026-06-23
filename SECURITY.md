# Security Policy

## Supported versions

Only the latest commit on `master` is supported with security
updates. Older tags and PRs are not maintained.

## Reporting a vulnerability

Please email security concerns to
`security@shiftleft-hub.example` (replace with the real address
before publishing). Do not file public GitHub issues for suspected
vulnerabilities.

Include in your report:

- A clear description of the issue and the impact.
- Steps to reproduce, or a proof-of-concept.
- Affected versions (commit SHA, tag, or branch).
- Your name / handle for credit (optional).

We aim to acknowledge new reports within 3 business days and to
disclose coordinated fixes within 90 days, sooner for critical
issues.

## Security model

- **Authentication:** JWT with HttpOnly cookies + refresh rotation.
  Access tokens are short-lived (15 min by default); refresh tokens
  are rotated on every use. There is no `localStorage` storage of
  credentials in the frontend.
- **Authorization:** role-based (`ROLE_USER`, `ROLE_AGENT`,
  `ROLE_ADMIN`) plus per-workspace membership checks via
  `WorkspaceMember`. Workspace-scoped resources are filtered at the
  repository level using the caller's user ID.
- **Secrets:** the backend reads secrets from environment variables
  (OpenAI keys, JWT signing keys, etc.) — there is no secrets file
  in the repository. See `application.properties` for the full list
  of env vars.
- **AI:** document chunks are stored in PostgreSQL with the
  pgvector extension. RAG similarity threshold is hard-floored at
  0.65 (see `AiConfigRequest`).

## Out of scope

- Denial-of-service attacks against the public Vercel preview
  deployment.
- Bugs in third-party dependencies — please report upstream.
