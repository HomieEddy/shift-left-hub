# Shift-Left Knowledge Hub - Testing Strategy Document (TSD)

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

### A. The "Golden Path" Strategy
E2E tests are brittle and slow if overused. We will write exactly **one** robust Playwright script that tests the critical "Happy Path" of the entire application to prove system integration.

* **The Scenario:** 1. User logs in.
  2. User types a query into the AI Assistant.
  3. User clicks "Escalate to Human".
  4. Agent logs in and sees the new ticket on the dashboard.
* **The Execution:** Playwright is chosen over older tools (Cypress/Protractor) to demonstrate up-to-date knowledge of modern web testing standards (multi-browser, auto-waiting, parallel execution).

```javascript
// Example: golden-path.spec.ts
import { test, expect } from '@playwright/test';

test('User can escalate issue and Agent receives ticket', async ({ page }) => {
  // 1. User Journey
  await page.goto('http://localhost:4200/login');
  await page.fill('input[name="email"]', 'user@shiftleft.com');
  await page.fill('input[name="password"]', 'password123');
  await page.click('button[type="submit"]');

  await page.fill('input[placeholder="Describe your issue..."]', 'VPN is down');
  await page.click('text="No, this didn\'t help (Escalate)"');
  await page.click('button:has-text("Submit Ticket")');
  
  await expect(page.locator('.toast-success')).toHaveText('Ticket Escalated');

  // 2. Agent Journey (Using a clean browser context)
  const agentContext = await page.context().browser().newContext();
  const agentPage = await agentContext.newPage();
  
  await agentPage.goto('http://localhost:4200/login');
  // ... agent login steps ...
  await expect(agentPage.locator('table tr')).toContainText('VPN is down');
});
```

---

## 4. CI/CD Integration (GitHub Actions)
These tests act as automated gatekeepers in the CI/CD pipeline mapped out in the Deployment Strategy.

1. **Backend CI:** Executes `mvn test` (running JUnit and Testcontainers). If any test fails, the build fails, and Railway does *not* deploy the new container.
2. **Frontend CI:** Executes `npm run test -- --watch=false`. If Jasmine tests fail, Vercel aborts the deployment to the Edge CDN.
3. **E2E Post-Deploy (Optional):** Playwright can be configured to run a quick health check against the live domain after a successful deployment to catch any environment-specific configuration errors.