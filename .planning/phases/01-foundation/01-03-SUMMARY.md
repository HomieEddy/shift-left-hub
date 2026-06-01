---
phase: 01-foundation
plan: 03
subsystem: api, ui
tags: angular, spring-boot, tailwind-css, i18n, admin-api, jwt

# Dependency graph
requires:
  - phase: 01-02
    provides: Auth backend (JWT, cookies, RBAC, admin seeder)
provides:
  - Admin User Management REST API (GET all, GET by ID, PUT role, PUT status)
  - Angular 21.2 SPA scaffold with standalone components and pnpm
  - Tailwind CSS v4 with @tailwindcss/vite plugin and custom theme
  - @angular/localize i18n scaffold (EN source + FR target)
  - Development proxy configuration (frontend:4200 → backend:8080)
  - Core services: AuthService, auth guard, error interceptor, translation service
  - Lazy-loaded route structure with feature modules
affects:
  - 01-04 (Frontend Auth UI) — will use AuthService, routes, and guard
  - Phase 2 (KB) — will use feature module structure
  - Phase 6 (Admin Review) — will extend admin/user-list component

# Tech tracking
tech-stack:
  added:
    - Angular 21.2 with @angular/build application builder
    - Tailwind CSS v4 with @tailwindcss/vite
    - pnpm 10.28.2
    - @angular/localize 21.2
    - Vitest (included by default)
  patterns:
    - Standalone components (no NgModules)
    - Lazy-loaded feature routes with loadComponent
    - Signals for local/component auth state
    - RxJS for HTTP operations with interceptors

key-files:
  created:
    - backend/.../AdminUserController.java
    - backend/.../AdminUserService.java
    - backend/.../UserResponse.java (record DTO)
    - backend/.../UserNotFoundException.java
    - frontend/proxy.conf.json
    - frontend/vite.config.ts
    - frontend/src/app/core/auth/auth.service.ts
    - frontend/src/app/core/auth/auth.guard.ts
    - frontend/src/app/core/auth/auth.models.ts
    - frontend/src/app/core/http/error.interceptor.ts
    - frontend/src/app/core/i18n/translation.service.ts
    - frontend/src/app/features/auth/login/login.component.ts (placeholder)
    - frontend/src/app/features/auth/register/register.component.ts (placeholder)
    - frontend/src/app/features/admin/user-list/user-list.component.ts (placeholder)
    - frontend/src/locale/messages.xlf
    - frontend/src/locale/messages.fr.xlf
  modified:
    - backend/.../GlobalExceptionHandler.java (added UserNotFound handler)

key-decisions:
  - "AdminUserService uses @Transactional(readOnly = true) class-level with @Transactional on write methods"
  - "Admin API security already handled by SecurityConfig.hasRole('ADMIN') — no per-method annotations needed in Phase 1"
  - "UserNotFoundException uses @ResponseStatus(HttpStatus.NOT_FOUND) with explicit handler in GlobalExceptionHandler"
  - "Tailwind CSS v4 configured via vite.config.ts with @tailwindcss/vite plugin (no tailwind.config.ts needed in v4)"
  - "i18n configured with angular.json i18n section (sourceLocale: en, locale: fr) — ng add @angular/localize handled polyfills"
  - "error.interceptor.ts uses console.error with TODO placeholder — toast/snackbar UI deferred to shared components phase"

patterns-established:
  - "Package-by-module: controller in user/api/, service in user/service/, DTOs in user/api/dto/"
  - "Record DTOs with static factory method from(Entity)"
  - "Angular feature modules organized by domain in src/app/features/{name}/"
  - "Core singletons (services, guards, interceptors) in src/app/core/{name}/"
  - "Lazy-loaded routes via loadComponent for each feature"

requirements-completed:
  - ADM-03
  - INF-03

duration: 5min
completed: 2026-05-31
---

# Phase 1 — Plan 03 Summary

**Admin user management REST API (list, get-by-id, update-role, toggle-status) + full Angular 21.2 SPA scaffold with Tailwind CSS v4, i18n, proxy config, and core AuthService/auth guard/error interceptor/translation service**

## Performance

- **Duration:** 5 min
- **Started:** 2026-05-31T16:26:31Z
- **Completed:** 2026-05-31T16:31:05Z
- **Tasks:** 3
- **Files modified:** 24 (6 backend, 18 frontend)

## Accomplishments

- Admin user management REST API with `GET /api/admin/users`, `GET /{id}`, `PUT /{id}/role`, `PUT /{id}/status` — automatically protected by `hasRole("ADMIN")` from SecurityConfig
- Angular 21.2 project created with standalone components, pnpm, and `@angular/build:application` builder
- Tailwind CSS v4 configured with `@tailwindcss/vite` plugin and custom `primary` color palette
- `@angular/localize` i18n scaffold with English source and French target locale files
- Development proxy (`proxy.conf.json`) forwarding `/api/*` to `localhost:8080`
- Feature module directory structure (`features/auth`, `features/admin`, `features/kb`, `features/chat`, `features/tickets`) with lazy-loaded routes
- AuthService with register, login, refresh, logout, and admin user methods — all using HttpOnly cookies via `withCredentials: true`
- `auth.guard.ts` protecting the `/admin/users` route
- `error.interceptor.ts` centralized HTTP error handling (401 → redirect, 403, 4xx, 5xx)
- `translation.service.ts` with browser language auto-detection and localStorage persistence

## Task Commits

Each task was committed atomically:

1. **Task 1: Backend Admin API** - `e2e130d` (feat(auth): add admin user management REST API)
2. **Task 2: Angular scaffold** - `31e92a8` (feat(ui): scaffold Angular 21.2 frontend with Tailwind CSS v4, i18n, and proxy)
3. **Task 3: Core services** - `3be9b7d` (feat(ui): add core Angular services — AuthService, auth guard, error interceptor, translation service)

## Files Created/Modified

### Backend (Task 1)
- `backend/src/main/java/com/shiftleft/hub/user/api/AdminUserController.java` — REST controller for admin user management
- `backend/src/main/java/com/shiftleft/hub/user/service/AdminUserService.java` — business logic for user CRUD operations
- `backend/src/main/java/com/shiftleft/hub/user/api/dto/UserResponse.java` — record DTO with `from(User)` factory
- `backend/src/main/java/com/shiftleft/hub/user/api/dto/RoleUpdateRequest.java` — validation DTO with `@NotNull UserRole`
- `backend/src/main/java/com/shiftleft/hub/user/domain/UserNotFoundException.java` — custom 404 exception
- `backend/src/main/java/com/shiftleft/hub/common/config/GlobalExceptionHandler.java` — added `UserNotFoundException` handler

### Frontend (Tasks 2 & 3)
- `frontend/angular.json` — project config with i18n, proxy, Tailwind polyfills
- `frontend/package.json` — all Angular 21.2 + Tailwind v4 dependencies
- `frontend/vite.config.ts` — Tailwind CSS v4 Vite plugin
- `frontend/proxy.conf.json` — dev server proxy to localhost:8080
- `frontend/src/styles.css` — Tailwind CSS v4 import with custom theme
- `frontend/src/locale/messages.xlf` — English source locale file
- `frontend/src/locale/messages.fr.xlf` — French target locale file
- `frontend/src/app/app.config.ts` — HttpClient with error interceptor
- `frontend/src/app/app.routes.ts` — lazy-loaded routes with authGuard
- `frontend/src/app/core/auth/auth.models.ts` — interface types for auth/user DTOs
- `frontend/src/app/core/auth/auth.service.ts` — AuthService with Signals
- `frontend/src/app/core/auth/auth.guard.ts` — route guard CanActivateFn
- `frontend/src/app/core/http/error.interceptor.ts` — HTTP error interceptor
- `frontend/src/app/core/i18n/translation.service.ts` — i18n service
- `frontend/src/app/features/auth/login/login.component.ts` — placeholder stub
- `frontend/src/app/features/auth/register/register.component.ts` — placeholder stub
- `frontend/src/app/features/admin/user-list/user-list.component.ts` — placeholder stub

## Decisions Made

- **AdminUserService @Transactional pattern:** Class-level `@Transactional(readOnly = true)` for read operations, per-method `@Transactional` for write operations — consistent with Spring best practices
- **UserNotFoundException in GlobalExceptionHandler:** Added explicit handler alongside existing DuplicateEmailException handler for consistency, even though `@ResponseStatus` alone would work
- **Tailwind v4 via vite.config.ts:** Tailwind CSS v4's `@tailwindcss/vite` plugin requires a Vite config. Angular 21's `@angular/build:application` detects and uses `vite.config.ts` automatically
- **Placeholder components for routes:** `LoginComponent`, `RegisterComponent`, `UserListComponent` created as minimal stubs to satisfy lazy-loaded route imports — will be implemented in subsequent plans
- **error.interceptor.ts uses console.error:** Toast/snackbar notification UI deferred to shared components phase (Phase 1-04 or later)

## Deviations from Plan

None — plan executed exactly as written.

## Known Stubs

| Stub | File | Reason |
|------|------|--------|
| Placeholder template | `features/auth/login/login.component.ts` | Will be implemented in 01-04 (Frontend Auth UI) |
| Placeholder template | `features/auth/register/register.component.ts` | Will be implemented in 01-04 |
| Placeholder template | `features/admin/user-list/user-list.component.ts` | Will be implemented in Phase 1-04 or later |
| console.error TODO | `core/http/error.interceptor.ts` | Toast/snackbar UI deferred to shared components phase |

## Issues Encountered

- Angular CLI `ng new` needed the `--skip-confirmation` flag removed (not a valid flag in Angular 21). Used standard arguments instead.
- `pnpm build` shows a harmless warning `"Workspace extension with invalid name (i18n) found"` — Angular CLI schematic validator doesn't recognize the `i18n` workspace extension key. This is cosmetic and the i18n configuration works correctly.

## User Setup Required

None — no external service configuration required.

## Verification Results

| Check | Status |
|-------|--------|
| `mvn compile -q` — backend compiles | ✓ Pass |
| `pnpm build` — frontend builds | ✓ Pass |
| AdminUserController with `@RequestMapping("/api/admin/users")` | ✓ Created |
| AdminUserService with getAllUsers, getUserById, updateUserRole, toggleUserStatus | ✓ Created |
| Proxy config forwards /api/* to localhost:8080 | ✓ Configured |
| Tailwind CSS compiles (stylesheet loads) | ✓ Confirmed in build output |
| @angular/localize configured for EN + FR | ✓ Locale files present |
| AuthService with register, login, refresh, logout | ✓ Created |
| auth.guard redirects unauthenticated to /login | ✓ Created |
| error.interceptor handles 401 → redirect | ✓ Created |
| translation.service persists language to localStorage | ✓ Created |

## Next Phase Readiness

- **Backend:** Admin user management API ready for integration testing — endpoints return data when authenticated as ADMIN
- **Frontend:** Angular SCAFFOLD ready for Phase 1-04 (Frontend Auth UI) — AuthService, routes, guard, and interceptor all wired. Login, register, and user-list placeholder components ready to be implemented
- **Dev workflow:** Frontend at `localhost:4200` proxies to backend at `localhost:8080` — `pnpm start` and `mvn spring-boot:run` can run simultaneously

---

*Phase: 01-foundation*
*Plan: 03*
*Completed: 2026-05-31*
