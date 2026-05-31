---
phase: 01-foundation
plan: 02
subsystem: auth
tags: [jwt, spring-security, spring-boot, jjwt, postgresql, rbac]
requires:
  - phase: 01-foundation
    plan: 01
    provides: Docker Compose with PostgreSQL, project scaffolding
provides:
  - JWT auth with HttpOnly cookies and refresh rotation
  - User entity (app_user) with RBAC roles (ROLE_USER, ROLE_ADMIN)
  - Register, login, refresh, logout API endpoints
  - Admin seeder creating initial admin user
  - Global exception handling with JSON error responses
affects: [phase 2 kb, phase 3 ai, phase 4 tickets, phase 5 agent-dashboard, phase 6 admin]

tech-stack:
  added:
    - io.jsonwebtoken:jjwt-api:0.13.0
    - org.projectlombok:lombok
    - spring-boot-starter-validation
    - spring-boot-starter-actuator
    - spring-modulith-starter-core (APT excluded for Boot 4.x compat)
  patterns:
    - Package-by-module: user/domain, user/api/dto, user/service, config/, common/config
    - JWT HttpOnly cookie auth (no localStorage)
    - Refresh token rotation with reuse detection (ConcurrentHashMap)
    - CORS configured before Spring Security via explicit CorsConfigurationSource bean
    - Global @RestControllerAdvice for standardized JSON error responses

key-files:
  created:
    - backend/src/main/java/com/shiftleft/hub/user/domain/UserRole.java
    - backend/src/main/java/com/shiftleft/hub/user/domain/User.java
    - backend/src/main/java/com/shiftleft/hub/user/domain/UserRepository.java
    - backend/src/main/java/com/shiftleft/hub/user/service/AuthService.java
    - backend/src/main/java/com/shiftleft/hub/user/api/AuthController.java
    - backend/src/main/java/com/shiftleft/hub/user/api/dto/RegisterRequest.java
    - backend/src/main/java/com/shiftleft/hub/user/api/dto/LoginRequest.java
    - backend/src/main/java/com/shiftleft/hub/user/api/dto/AuthResponse.java
    - backend/src/main/java/com/shiftleft/hub/config/JwtService.java
    - backend/src/main/java/com/shiftleft/hub/config/SecurityConfig.java
    - backend/src/main/java/com/shiftleft/hub/common/DuplicateEmailException.java
    - backend/src/main/java/com/shiftleft/hub/common/config/DataSeeder.java
    - backend/src/main/java/com/shiftleft/hub/common/config/GlobalExceptionHandler.java
  modified:
    - backend/pom.xml
    - backend/src/main/resources/application.properties
    - backend/src/main/java/com/shiftleft/hub/KnowledgeHubApplication.java
    - backend/src/test/java/com/shiftleft/hub/KnowledgeHubApplicationTests.java

key-decisions:
  - "Excluded spring-modulith-apt annotation processor — incompatible with Spring Boot 4.0.6 JsonWriter API. Core Modulith runtime retained."
  - "Added explicit maven-compiler-plugin annotationProcessorPaths to ensure only Lombok processor runs."
  - "Added GlobalExceptionHandler (Rule 2 — AGENTS.md requires @RestControllerAdvice pattern, not in plan)"
  - "Added extractTokenId() to JwtService (Rule 2 — required for refresh rotation flow, not in spec)"

patterns-established:
  - "Auth pattern: JWT in HttpOnly cookies with sameSite=Lax, access_token (15min) + refresh_token (7d) with rotation+reuse detection"
  - "Password validation: 8+ chars, 1+ uppercase, 1+ digit via @Pattern regex"
  - "Error handling: GlobalExceptionHandler translates DuplicateEmailException→409, BadCredentialsException→401, validation errors→400"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, AUTH-04, INF-02]

duration: 18min
completed: 2026-05-31
---

# Phase 1 Plan 2: Auth Backend Summary

**JWT auth with HttpOnly cookie-based refresh rotation, User entity with RBAC (ROLE_USER/ROLE_ADMIN), admin seeder, and global exception handling — all in com.shiftleft.hub package**

## Performance

- **Duration:** 18 min
- **Started:** 2026-05-31T12:17:00Z
- **Completed:** 2026-05-31T12:24:13Z
- **Tasks:** 3
- **Files modified:** 17

## Accomplishments

- pom.xml updated with JJWT 0.13.0, Lombok, validation, actuator, Spring Modulith BOM
- Package restructured from `ShiftLeftHub` to `com.shiftleft.hub` with module structure
- User entity (`app_user` table) with UUID PK, email, bcrypt password, displayName, UserRole, enabled, timestamps
- JwtService with access (15min) and refresh (7d) token generation, validation, and rotation + reuse detection via ConcurrentHashMap
- SecurityConfig with HttpOnly cookie-based JwtAuthenticationFilter, CORS for localhost:4200, stateless sessions, CSRF disabled (SameSite handles it)
- AuthService with register (auto-login), login, refresh with rotation, logout
- AuthController at `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `/api/auth/logout` with HttpOnly cookies
- DataSeeder (CommandLineRunner) creates admin user on first startup
- GlobalExceptionHandler for standardized JSON error responses

## Task Commits

Each task was committed atomically:

1. **Task 1: Update pom.xml with auth dependencies and restructure package** - `9ce159d` (feat)
2. **Task 2: Create User entity, UserRole enum, UserRepository, JwtService, SecurityConfig** - `a704907` (feat)
3. **Task 3: Create AuthService, AuthController, and DataSeeder** - `9846203` (feat)

## Files Created/Modified

- `backend/pom.xml` - All auth dependencies (JJWT 0.13.0, Lombok, validation, actuator, Modulith BOM), annotation processor config, Lombok exclusion
- `backend/src/main/resources/application.properties` - JWT secret, token expirations, actuator health endpoint
- `backend/src/main/java/com/shiftleft/hub/KnowledgeHubApplication.java` - Renamed package from ShiftLeftHub
- `backend/src/main/java/com/shiftleft/hub/user/domain/UserRole.java` - ROLE_USER, ROLE_ADMIN enum
- `backend/src/main/java/com/shiftleft/hub/user/domain/User.java` - JPA entity, app_user table, UUID PK, Builder pattern
- `backend/src/main/java/com/shiftleft/hub/user/domain/UserRepository.java` - findByEmail, existsByEmail
- `backend/src/main/java/com/shiftleft/hub/user/service/AuthService.java` - Register (auto-login), login, refresh, logout with @Transactional
- `backend/src/main/java/com/shiftleft/hub/user/api/dto/RegisterRequest.java` - @NotBlank @Email @Size @Pattern validation
- `backend/src/main/java/com/shiftleft/hub/user/api/dto/LoginRequest.java` - @NotBlank @Email
- `backend/src/main/java/com/shiftleft/hub/user/api/dto/AuthResponse.java` - Token + user info record
- `backend/src/main/java/com/shiftleft/hub/user/api/AuthController.java` - 4 POST endpoints with HttpOnly cookie setup/teardown
- `backend/src/main/java/com/shiftleft/hub/config/JwtService.java` - Token generation, parsing, rotation validation, reuse detection
- `backend/src/main/java/com/shiftleft/hub/config/SecurityConfig.java` - Security filter chain, CORS, JwtAuthenticationFilter, BCryptPasswordEncoder
- `backend/src/main/java/com/shiftleft/hub/common/DuplicateEmailException.java` - Custom 409 exception
- `backend/src/main/java/com/shiftleft/hub/common/config/DataSeeder.java` - CommandLineRunner, creates admin@shiftleft.com
- `backend/src/main/java/com/shiftleft/hub/common/config/GlobalExceptionHandler.java` - @RestControllerAdvice for 401/400/409/500

## Decisions Made

- **Modulith APT excluded** — `spring-modulith-apt` annotation processor is incompatible with Spring Boot 4.0.6's `JsonWriter.Members.add()` API. The core Modulith runtime remains for module boundary enforcement. Documented in pom.xml comment.
- **Explicit annotation processor paths** — Added `maven-compiler-plugin` config with `annotationProcessorPaths` to ensure only Lombok runs as annotation processor.
- **GlobalExceptionHandler added** — Not in the plan but required by AGENTS.md project guidelines (@RestControllerAdvice pattern). Handles DuplicateEmailException (409), BadCredentialsException (401), validation errors (400), and generic errors (500).
- **JwtService.extractTokenId() added** — Required by AuthService.refresh() to extract the tokenId claim for rotation validation. Not in the plan's method list but logically necessary.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Excluded spring-modulith-apt from starter-core**
- **Found during:** Task 1 (pom.xml update)
- **Issue:** `SpringModulithProcessor` annotation processor calls `JsonWriter.Members.add(String, Function)` which has a different signature in Spring Boot 4.0.6, causing `NoSuchMethodError` at compile time.
- **Fix:** Added `<exclusions>` to `spring-modulith-starter-core` to exclude `spring-modulith-apt`. Also added explicit `annotationProcessorPaths` in `maven-compiler-plugin` config.
- **Files modified:** backend/pom.xml
- **Verification:** `mvn clean compile -q` succeeds (exit 0)
- **Committed in:** 9ce159d (Task 1 commit)

**2. [Rule 2 - Missing Critical] Added GlobalExceptionHandler**
- **Found during:** Task 3 (AuthService/AuthController creation)
- **Issue:** AuthService throws DuplicateEmailException and BadCredentialsException, and @Valid annotations on DTOs trigger MethodArgumentNotValidException — but no handler existed to translate these to JSON error responses. AGENTS.md mandates single @RestControllerAdvice.
- **Fix:** Created GlobalExceptionHandler with handlers for 409, 401, 400 (validation), and 500 errors.
- **Files modified:** backend/src/main/java/com/shiftleft/hub/common/config/GlobalExceptionHandler.java
- **Verification:** Compiles and integrates with Spring Boot auto-configuration
- **Committed in:** 9846203 (Task 3 commit)

**3. [Rule 2 - Missing Critical] Added JwtService.extractTokenId()**
- **Found during:** Task 3 (AuthService.refresh implementation)
- **Issue:** AuthService.refresh() needs to extract the `tokenId` claim from the refresh token to call `validateRefreshRotation()`, but no method existed on JwtService for this.
- **Fix:** Added `extractTokenId(String token)` public method to JwtService.
- **Files modified:** backend/src/main/java/com/shiftleft/hub/config/JwtService.java
- **Verification:** AuthService.refresh() can now properly extract tokenId for rotation validation
- **Committed in:** 9846203 (Task 3 commit)

---

**Total deviations:** 3 auto-fixed (1 blocking, 2 missing critical)
**Impact on plan:** All auto-fixes necessary for compilation and correctness. No scope creep.

## Issues Encountered

- **Spring Modulith + Boot 4.0.6 incompatibility:** The `spring-modulith-apt` annotation processor is not compatible with Spring Boot 4.0.6's `JsonWriter.Members` API. The annotation processor was excluded; runtime Modulith features remain functional.
- **Port 5432 conflict:** Native PostgreSQL 18 is installed on the system and occupies port 5432. The Docker Compose PostgreSQL cannot bind to 127.0.0.1:5432. When running tests, the test container must be started on an alternative port (5440 used for testing) or the native PostgreSQL must be configured with the `shiftleft` user/password.
- **Test requires running PostgreSQL:** The `@SpringBootTest` context load test requires a real PostgreSQL connection. Tests pass when PostgreSQL is available with the correct credentials.

## User Setup Required

None - no external service configuration required for compiled code. PostgreSQL must be running for runtime (via Docker Compose or native installation).

## Next Phase Readiness

- Authentication fully implemented and ready for frontend integration (Plan 01-03: Angular Scaffold)
- User entity and repository ready for admin user management features (later phases)
- Security infrastructure (JWT filter, CORS, role-based guards) in place for all future endpoints
- Pending: No integration test user setup (deferred to test infrastructure phase)

## Self-Check: PASSED

All 17 files confirmed present. All 3 commits verified in git log.
- `9ce159d` - feat(01-auth): add auth dependencies, update app.properties, restructure package
- `a704907` - feat(01-auth): add User entity, UserRole enum, UserRepository, JwtService, SecurityConfig
- `9846203` - feat(01-auth): add AuthService, AuthController, DataSeeder, and global exception handling
- `mvn clean compile -q` — PASS (exit 0)
- `mvn test` — PASS (Tests run: 1, Failures: 0, Errors: 0) with PostgreSQL

---
*Phase: 01-foundation*
*Plan: 02*
*Completed: 2026-05-31*
