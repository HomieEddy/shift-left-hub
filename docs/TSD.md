# Shift-Left Knowledge Hub - Testing Strategy Document (TSD)

> **Updated:** 2026-06-14 — v2.1 Deployment milestone, Phase 19 E2E refactor.
> Current coverage: 112 backend integration tests + 9 backend unit tests + 127 frontend tests + 8 Playwright E2E specs.
> Phase 19 replaced the single golden-path with 8 independent per-feature E2E specs.

## The Philosophy: Pragmatic Competency
To demonstrate engineering maturity without over-engineering a portfolio project, this testing strategy focuses strictly on High-ROI (Return on Investment) tests. We will ignore trivial tests (like testing getters/setters) and focus on the critical paths: business logic in the backend, state management in the frontend, and one "Golden Path" End-to-End (E2E) test.

---

## 1. Backend Testing (Spring Boot 3.x)

### A. Unit Testing the Service Layer (JUnit 5 + Mockito)
Since our Clean Code Guidelines mandate that controllers contain zero business logic, we will skip Controller tests and focus entirely on the Service layer.

* **The Strategy:** Use `@ExtendWith(MockitoExtension.class)` to mock dependencies (like Repositories and the Spring AI `ChatClient`).
* **The Execution:** Do not call the real OpenAI API during tests. Mock the AI response to ensure tests run fast, deterministically, and cost nothing.

```java
// Example: TicketServiceTest.java
@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private TicketRepository ticketRepository;
    @InjectMocks private TicketService ticketService;

    @Test
    void shouldCreateTicketWhenEscalated() {
        // Arrange
        TicketCreateRequest request = new TicketCreateRequest("VPN broken", "HIGH");
        when(ticketRepository.save(any(Ticket.class))).thenReturn(new Ticket(1L));

        // Act
        TicketResponse response = ticketService.escalateTicket(request);

        // Assert
        assertNotNull(response.id());
        verify(ticketRepository, times(1)).save(any(Ticket.class));
    }
}
```

### B. Integration Testing the Database (Testcontainers)
* **The Strategy:** Do **not** use an H2 in-memory database for testing. H2 does not support native PostgreSQL features like `JSONB` or `TSVECTOR` (which we rely on for Full-Text Search).
* **The Execution:** Use **Testcontainers**. This library spins up a real, ephemeral PostgreSQL Docker container specifically for your test suite, ensuring 100% environment parity between your tests and production.

---

## 2. Frontend Testing (Angular)

### A. Unit Testing (Jasmine + Karma / Jest)
Avoid testing "Dumb" UI components (like ensuring a button is blue). Focus on the "Smart" components and Angular Services that handle RxJS streams and API calls.

* **The Strategy:** Test the `TicketService` to ensure HTTP calls are formatted correctly and test the `Debounce` logic on the Search Input to ensure the frontend isn't spamming the backend API.
* **The Execution:** Use `HttpTestingController` to mock API responses natively within Angular, preventing the frontend tests from actually hitting the network.

```typescript
// Example: ticket.service.spec.ts
it('should fetch tickets and return an Observable', () => {
  const mockTickets: Ticket[] = [{ id: 1, status: 'NEW' }];

  service.getTickets().subscribe(tickets => {
    expect(tickets.length).toBe(1);
    expect(tickets[0].status).toEqual('NEW');
  });

  const req = httpTestingController.expectOne('/api/tickets');
  expect(req.request.method).toEqual('GET');
  req.flush(mockTickets); // Simulate backend response
});
```

---

## 3. End-to-End (E2E) Testing (Playwright)

### A. Per-Feature Spec Strategy
E2E tests are brittle and slow if overused. We write **one spec per user-facing feature** (8 total) to prove system integration while keeping each test focused and independent.

* **The Scenarios:** Each spec covers a distinct feature's happy path:
  1. Auth (TST-08): register, login, logout, protected route redirect
  2. Knowledge Base (TST-09): browse, search, view article, bilingual switch
  3. AI Chat (TST-10): ask question, receive response, rate answer
  4. Escalation (TST-11): user creates ticket, agent resolves
  5. Agent Dashboard (TST-12): view queue, claim, resolve with KCS flag
  6. Workspace Management (TST-13): switch workspace, view members
  7. Admin (TST-14): manage users, manage tags, approve/reject KCS drafts
  8. Document Ingestion (TST-15): upload, verify indexed, query, cleanup
* **The Execution:** Each spec is fully independent per D-06 (no cross-test state dependencies). AI response assertions use presence-only checks per D-09. Playwright is chosen over older tools (Cypress/Protractor) to demonstrate up-to-date knowledge of modern web testing standards (multi-browser, auto-waiting, parallel execution).

**Page objects** (`e2e/pages/`) encapsulate per-feature selectors and interactions:
- `LoginPage`, `ChatPage`, `TicketsPage`, `AgentDashboardPage`, `KcsDraftsPage`, `KnowledgeBasePage`
- `WorkspaceManagementPage`, `AdminPage`, `DocumentsPage`

---

## 4. CI/CD Integration (GitHub Actions)
These tests act as automated gatekeepers in the CI/CD pipeline mapped out in the Deployment Strategy.

1. **Backend CI:** Executes `mvn test` (running JUnit and Testcontainers). If any test fails, the build fails, and Railway does *not* deploy the new container.
2. **Frontend CI:** Executes `npm run test -- --watch=false`. If Jasmine tests fail, Vercel aborts the deployment to the Edge CDN.
3. **E2E Post-Deploy (Optional):** Playwright can be configured to run a quick health check against the live domain after a successful deployment to catch any environment-specific configuration errors.