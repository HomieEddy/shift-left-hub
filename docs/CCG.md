# Shift-Left Knowledge Hub - Clean Code Guidelines (CCG)

> **Updated:** 2026-06-14 — v2.0 Workspace Platform milestone.
> All guidelines remain in effect. Key v2.0 patterns: workspace-scoped services use `WorkspaceRoleService` for role checks, new modules use `@Input`/`@Output` Dumb components, and Hibernate `@Filter` + AOP enforce data isolation on the backend.

## 1. General Architectural Principles
Before writing framework-specific code, all commits must adhere to these foundational principles:
* **SOLID Principles:** Follow Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, and Dependency Inversion to ensure the monolith remains modular and testable.
* **KISS (Keep It Simple, Stupid):** Avoid clever, unreadable one-liners. Code should read like well-structured English.
* **YAGNI (You Aren't Gonna Need It):** Do not build abstractions, interfaces, or generic services for use cases that do not exist yet. Build for today's requirements.
* **DRY (Don't Repeat Yourself):** If you copy-paste code more than twice, extract it into a shared utility function or service.
* **Boy Scout Rule:** Always leave the codebase cleaner than you found it. 

---

## 2. Spring Boot 3.x (Backend) Standards

### A. Layered Architecture Strictness
Never bypass the architectural layers. 
* **Controllers:** Handle HTTP requests/responses, validate inputs, and route to services. **Absolutely zero business logic.**
* **Services:** Contain 100% of the business logic and transaction management (`@Transactional`).
* **Repositories:** Only interact with the database. No business logic.

### B. The DTO (Data Transfer Object) Pattern
**Never expose JPA Entities directly to the API.** Returning an `@Entity` from a REST controller couples your database schema to your API contract and leads to infinite recursion with bidirectional relationships.
* Create specific Request and Response DTOs (e.g., `TicketCreateRequest`, `TicketResponse`).
* Use Java 14+ `record` types for DTOs to keep them immutable and boilerplate-free.

### C. Dependency Injection
**Ban field injection (`@Autowired` on variables).** Always use Constructor Injection. It makes classes easier to mock in unit tests and ensures dependencies are immutable.
```java
// ❌ BAD (Field Injection)
@Autowired
private TicketService ticketService;

// ✅ GOOD (Constructor Injection via Lombok)
@Service
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;
}
```

### D. Global Exception Handling
Do not clutter Controllers with `try-catch` blocks. 
* Throw custom domain exceptions from the Service layer (e.g., `ResourceNotFoundException`).
* Use a single `@RestControllerAdvice` class to catch these exceptions and translate them into standardized JSON error responses with appropriate HTTP status codes (404, 400, 403).

---

## 3. Angular (Frontend) Standards

### A. RxJS & Subscription Management
Memory leaks are the number one killer of Angular apps.
* **The Golden Rule:** Prefer the `async` pipe in the HTML template over calling `.subscribe()` in the TypeScript file. The `async` pipe automatically unsubscribes when the component is destroyed.
* **If you MUST subscribe in TS:** Always use `takeUntilDestroyed()` (Angular 16+) or the `Subject` destroy pattern to clean up subscriptions.

```html
<div *ngFor="let ticket of tickets$ | async">
  {{ ticket.title }}
</div>
```

### B. Smart vs. Dumb Components
* **Smart Components (Features):** These inject services, communicate with the API via RxJS, and hold state. They pass data down to Dumb components.
* **Dumb Components (UI/Shared):** These only receive data via `@Input()` and emit events via `@Output()`. They have no dependencies on external data services. This makes them highly reusable.

### C. Ban "any" (Strong Typing)
TypeScript is used for a reason. **Using `any` is strictly prohibited.**
* Always define Interfaces or Types for your payloads (e.g., `Ticket`, `User`).
* If you do not know the exact shape of an object yet, use `unknown` instead of `any`, which forces you to type-check before accessing properties.

### D. Keep Logic Out of Templates
Do not put complex JavaScript logic, calculations, or method calls inside HTML interpolation `{{ }}`.
* Angular evaluates template expressions on every change detection cycle. Calling a heavy function in the HTML will destroy application performance.
* **Solution:** Calculate the value in the TypeScript controller and bind the result to a simple property, or use an Angular Pipe.

```html
<div *ngIf="checkIfUserHasAdminPrivilegesAndIsActive(user)">

<div *ngIf="isAdminActive">
```

### E. File Naming Conventions
Stick strictly to the Angular Style Guide (Kebab-case).
* **Components:** `ticket-list.component.ts`
* **Services:** `ticket.service.ts`
* **Models:** `ticket.model.ts`