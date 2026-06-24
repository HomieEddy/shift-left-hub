# Shift-Left Knowledge Hub - Testing Strategy Document (TSD)

> **Updated:** 2026-06-23 — v2.2 Post-Cleanup milestone.
> Current coverage: 469 backend tests (JUnit 5 + Testcontainers) + 306 frontend tests (Vitest) + 10 Playwright E2E specs.
> The runner is Vitest (not Karma) and the build target is Spring Boot 4 (not 3.x).

## The Philosophy: Pragmatic Competency

To demonstrate engineering maturity without over-engineering a portfolio project, this testing strategy focuses strictly on High-ROI (Return on Investment) tests. We ignore trivial tests (like testing getters/setters) and focus on the critical paths: business logic in the backend, state management in the frontend, and one "Golden Path" End-to-End (E2E) test.

---

## 1. Backend Testing (Spring Boot 4.x)

### A. Unit Testing the Service Layer (JUnit 5 + Mockito)

Since our Clean Code Guidelines mandate that controllers contain zero business logic, we skip Controller tests and focus entirely on the Service layer.

- **The Strategy:** Use `@ExtendWith(MockitoExtension.class)` to mock dependencies (like Repositories and the Spring AI `ChatClient`).
- **The Execution:** Do not call the real OpenAI API during tests. Mock the AI response to ensure tests run fast, deterministically, and cost nothing.

```java
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @InjectMocks private TicketService ticketService;

    @Test
    void shouldCreateTicketWhenEscalated() {
        TicketCreateRequest request = new TicketCreateRequest("VPN broken", "HIGH");
        when(ticketRepository.save(any(Ticket.class))).thenReturn(new Ticket(UUID.randomUUID()));

        TicketResponse response = ticketService.escalateTicket(request);

        assertNotNull(response.id());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }
}
```

### B. Integration Testing the Database (Testcontainers)

- **The Strategy:** Do **not** use an H2 in-memory database for testing. H2 does not support native PostgreSQL features like `JSONB` or `TSVECTOR` (which we rely on for Full-Text Search).
- **The Execution:** Use **Testcontainers**. This library spins up a real, ephemeral PostgreSQL 16 Docker container (image `pgvector/pgvector:0.8.0-pg16`) for the test suite, ensuring 100% environment parity between tests and production.
- **Class-level annotation:** Tests that need the database are annotated with `@Testcontainers` + `@Container static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:0.8.0-pg16")`.

### C. Test Naming Convention

`MethodUnderTest_expectedBehavior_whenCondition` — e.g. `createTicket_shouldThrowWhenTitleBlank`, `updateUserRole_shouldThrowWhenSelfDemote`. This makes failure output self-documenting in CI.

---

## 2. Frontend Testing (Angular 21 — Vitest)

> **Note:** The runtime is Vitest with a JSDOM environment, not Karma. `ng test` invokes Vitest under the hood. `tsconfig.spec.json` declares `"types": ["vitest/globals", "@angular/localize"]` so Vitest's `describe`/`it`/`expect`/`vi` are available globally. Jasmine-style tests (`describe`/`it`/`expect`) work unchanged; mocks use `vi.fn()` and `vi.spyOn()`.

### A. What to Test

Skip "Dumb" UI components (like ensuring a button is blue). Focus on "Smart" components and Angular Services that handle RxJS streams and API calls.

- **The Strategy:** Test the `TicketService` to ensure HTTP calls are formatted correctly and test the `Debounce` logic on the Search Input to ensure the frontend isn't spamming the backend API.
- **The Execution:** Use `HttpTestingController` to mock API responses natively within Angular, preventing the frontend tests from actually hitting the network.

```typescript
it('should fetch tickets and return an Observable', () => {
  const mockTickets: Ticket[] = [{ id: 1, status: 'NEW' }];

  service.getTickets().subscribe(tickets => {
    expect(tickets.length).toBe(1);
    expect(tickets[0].status).toEqual('NEW');
  });

  const req = httpTestingController.expectOne('/api/tickets');
  expect(req.request.method).toEqual('GET');
  req.flush(mockTickets);
});
```

### B. Guard / Route Testing

For `canMatch` guards, test the function directly via `TestBed.runInInjectionContext` with `provideRouter([{ path: 'x', canMatch: [guard], children: [] }])`, and spy on `Router.navigate` (the `Router` from `TestBed.inject(Router)`) to assert redirect targets. Observable returns from the guard must be subscribed to directly — `TestBed.runInInjectionContext` does not subscribe automatically.

### C. Run Commands

```sh
cd frontend
pnpm test -- --watch=false        # one-shot run
pnpm test -- --watch              # watch mode for local dev
```

---

## 3. End-to-End (E2E) Testing (Playwright)

### A. Per-Feature Spec Strategy

E2E tests are brittle and slow if overused. We write **one Golden Path spec + 9 exploratory specs** to prove system integration while keeping each test focused and independent.

- **The Scenarios:** Each spec covers a distinct feature's happy path:
  1. `golden-path.spec.ts` (CI-required) — login → AI query → escalate to human → agent receives ticket, using two browser contexts (User + Agent)
  2. `auth.spec.ts` (TST-08): register, login, logout, protected route redirect
  3. `kb.spec.ts` (TST-09): browse, search, view article, bilingual switch
  4. `ai-chat.spec.ts` (TST-10): ask question, receive response, rate answer
  5. `escalation.spec.ts` (TST-11): user creates ticket, agent resolves
  6. `agent-dashboard.spec.ts` (TST-12): view queue, claim, resolve with KCS flag
  7. `workspace-management.spec.ts` (TST-13): switch workspace, view members
  8. `admin.spec.ts` (TST-14): manage users, manage tags, approve/reject KCS drafts
  9. `document-ingestion.spec.ts` (TST-15): upload, verify indexed, query, cleanup
- **The Execution:** Each spec is fully independent (no cross-test state dependencies). AI response assertions use presence-only checks. Playwright is chosen over older tools (Cypress/Protractor) for multi-browser, auto-waiting, parallel execution.

**Page objects** (`e2e/pages/`) encapsulate per-feature selectors and interactions:

`LoginPage`, `ChatPage`, `TicketsPage`, `AgentDashboardPage`, `KcsDraftsPage`, `KnowledgeBasePage`, `WorkspaceManagementPage`, `AdminPage`, `DocumentsPage`.

### B. CI Integration

The Golden Path spec is the only E2E test required to pass in CI. The 9 exploratory specs are non-gating and run on demand.

---

## 4. CI/CD Integration (GitHub Actions)

These tests act as automated gatekeepers in the CI/CD pipeline mapped out in `DHS.md`.

1. **Backend CI:** Executes `mvn test` (running JUnit and Testcontainers). If any test fails, the build fails, and Railway does *not* deploy the new container.
2. **Frontend CI:** Executes `pnpm test -- --watch=false`. If Vitest tests fail, Vercel aborts the deployment.
3. **Post-Deploy (Optional):** Playwright can be configured to run a quick health check against the live domain after a successful deployment to catch any environment-specific configuration errors.

---

## 5. Test Counts (as of v2.2)

| Layer | Count | Note |
|-------|-------|------|
| Backend | 469 | 357 unit + 112 integration (Testcontainers) |
| Frontend | 306 | Vitest + HttpTestingController, no Karma |
| E2E specs | 10 | 1 Golden Path (CI-required) + 9 exploratory (non-gating) |

These counts are reported in each PR's body. If you change them, update `README.md` and the relevant milestone summary in `.planning/milestones/`.
