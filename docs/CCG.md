# Shift-Left Knowledge Hub - Clean Code Guidelines (CCG)

> **Updated:** 2026-06-23 — v2.2 Post-Cleanup milestone.
> Spring Boot 4 is the current backend major (3.x references updated).
> Typed domain exceptions are the standard for new services (`@RestControllerAdvice` in `GlobalExceptionHandler` translates to RFC 7807 `ProblemDetail`).

## 1. General Architectural Principles

Before writing framework-specific code, all commits must adhere to these foundational principles:

- **SOLID Principles:** Follow Single Responsibility, Open-Closed, Liskov Substitution, Interface Segregation, and Dependency Inversion to ensure the monolith remains modular and testable.
- **KISS (Keep It Simple, Stupid):** Avoid clever, unreadable one-liners. Code should read like well-structured English.
- **YAGNI (You Aren't Gonna Need It):** Do not build abstractions, interfaces, or generic services for use cases that do not exist yet. Build for today's requirements.
- **DRY (Don't Repeat Yourself):** If you copy-paste code more than twice, extract it into a shared utility function or service.
- **Boy Scout Rule:** Always leave the codebase cleaner than you found it.

---

## 2. Spring Boot 4.x (Backend) Standards

### A. Layered Architecture Strictness

Never bypass the architectural layers.

- **Controllers:** Handle HTTP requests/responses, validate inputs, and route to services. **Absolutely zero business logic.**
- **Services:** Contain 100% of the business logic and transaction management (`@Transactional`).
- **Repositories:** Only interact with the database. No business logic.

### B. The DTO (Data Transfer Object) Pattern

**Never expose JPA Entities directly to the API.** Returning an `@Entity` from a REST controller couples your database schema to your API contract and leads to infinite recursion with bidirectional relationships.

- Create specific Request and Response DTOs (e.g. `TicketCreateRequest`, `TicketResponse`).
- Use Java 14+ `record` types for DTOs to keep them immutable and boilerplate-free.
- For DTOs that are projections of an entity plus an externally-computed field (counts, derived booleans), add a static `from(Entity, …)` factory on the DTO — service code calls the factory, never `new DtoRecord(entity.field1, …)` directly.

### C. Dependency Injection

**Ban field injection (`@Autowired` on variables).** Always use Constructor Injection. It makes classes easier to mock in unit tests and ensures dependencies are immutable.

```java
// BAD: Field injection
@Autowired
private TicketService ticketService;

// GOOD: Constructor injection via Lombok
@Service
@RequiredArgsConstructor
public class TicketController {
    private final TicketService ticketService;
}
```

### D. Global Exception Handling

Do not clutter Controllers with `try-catch` blocks.

- Throw custom domain exceptions from the Service layer (e.g. `TicketNotFoundException`, `SelfModificationException`).
- A single `@RestControllerAdvice` (`GlobalExceptionHandler`) catches these and translates them to RFC 7807 `ProblemDetail` responses with the right HTTP status code.
- **Typed exceptions are mandatory** for new service methods. A `RuntimeException("not found")` is a code smell; the `NotFoundException` in the right module is the right answer.
- The handler returns `ProblemDetail` with a stable `type` URI (e.g. `urn:shiftleft:problem:invitation-not-found`) so the frontend can switch on the type instead of parsing the message.

Common status codes:

| Status | Exception | Example |
|--------|-----------|---------|
| 400 | `@Valid` failure, `IllegalArgumentException` | malformed DTO, validation floor |
| 401 | `AdminNotFoundException` | admin token not in DB |
| 404 | `XxxNotFoundException` | entity not present |
| 409 | `LastAdminException`, `SelfModificationException`, `DuplicateXxxException` | domain conflict |

---

## 3. Angular (Frontend) Standards

### A. Routing Guards: `canMatch` not `canActivate`

All role-restricted routes use `canMatch` (introduced v2.2). With `canMatch`, the router does not even attempt to download the lazy chunk if the user is unauthorized. See `app.routes.ts` for the 18 protected routes.

### B. Standalone Components and Signals

- New code uses standalone components (`standalone: true`), not NgModules.
- State is signal-based (`signal()`, `computed()`, `effect()`). RxJS is for streams; signals are for derived state.
- Public signal inputs are read-only via `.asReadonly()` when they should not be mutated by consumers.

### C. RxJS & Subscription Management

Memory leaks are the number one killer of Angular apps.

- **The Golden Rule:** Prefer the `async` pipe in the HTML template over calling `.subscribe()` in the TypeScript file. The `async` pipe automatically unsubscribes when the component is destroyed.
- **If you MUST subscribe in TS:** Always use `takeUntilDestroyed(this.destroyRef)` (Angular 16+) or the `Subject` destroy pattern to clean up subscriptions.

```html
<div *ngFor="let ticket of tickets$ | async">
  {{ ticket.title }}
</div>
```

### D. Smart vs. Dumb Components

- **Smart Components (Features):** These inject services, communicate with the API via RxJS, and hold state. They pass data down to Dumb components.
- **Dumb Components (UI/Shared):** These only receive data via `@Input()` and emit events via `@Output()`. They have no dependencies on external data services. This makes them highly reusable.

### E. Ban "any" (Strong Typing)

TypeScript is used for a reason. **Using `any` is strictly prohibited.**

- Always define Interfaces or Types for your payloads (e.g. `Ticket`, `User`).
- If you do not know the exact shape of an object yet, use `unknown` instead of `any`, which forces you to type-check before accessing properties.

### F. Route Param Subscription

For components that read URL params (`:id`, `:slug`), subscribe to `route.paramMap` in `ngOnInit` instead of reading `route.snapshot.paramMap.get('id')`. Angular reuses a component instance when only the param changes (e.g. `/admin/articles/A` → `/admin/articles/B`), so `ngOnInit` does not re-fire and the snapshot stays stale.

```typescript
this.route.paramMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe((params) => {
  const id = params.get('id');
  if (id !== null && id !== '') {
    this.load(id);
  }
});
```

### G. Keep Logic Out of Templates

Do not put complex JavaScript logic, calculations, or method calls inside HTML interpolation `{{ }}`.

- Angular evaluates template expressions on every change detection cycle. Calling a heavy function in the HTML will destroy application performance.
- **Solution:** Calculate the value in the TypeScript controller and bind the result to a simple property, or use an Angular Pipe.

### H. File Naming Conventions

Stick strictly to the Angular Style Guide (kebab-case).

- **Components:** `ticket-list.component.ts`
- **Services:** `ticket.service.ts`
- **Models:** `ticket.model.ts`
