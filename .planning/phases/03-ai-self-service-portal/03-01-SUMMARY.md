---
phase: 03-ai-self-service-portal
plan: 01
completed: true
date: 2026-06-03
commits:
  - "30cd288"
status: complete
---

# Plan 03-01: Backend AI Infrastructure

## What Was Built

Spring AI infrastructure foundation: dependency setup, LLM provider config storage, embedding service, and admin REST API for AI settings.

## Key Files Created

| File | Purpose |
|------|---------|
| `backend/pom.xml` (modified) | Spring AI 2.0.0-M8 BOM + Ollama/OpenAI/pgvector starters |
| `backend/src/main/java/com/shiftleft/hub/ai/domain/AiConfig.java` | Single-row JPA entity with encrypted API key |
| `backend/src/main/java/com/shiftleft/hub/ai/domain/AiConfigRepository.java` | JPA repository with single-config query |
| `backend/src/main/java/com/shiftleft/hub/ai/domain/package-info.java` | @NullMarked annotation |
| `backend/src/main/java/com/shiftleft/hub/ai/api/dto/AiConfigRequest.java` | Config update DTO |
| `backend/src/main/java/com/shiftleft/hub/ai/api/dto/AiConfigResponse.java` | Config response DTO (masks API key) |
| `backend/src/main/java/com/shiftleft/hub/ai/api/dto/TestConnectionResult.java` | Connection test result DTO |
| `backend/src/main/java/com/shiftleft/hub/ai/api/AiConfigController.java` | REST API: GET/PUT config, POST test, POST reindex |
| `backend/src/main/java/com/shiftleft/hub/ai/service/AiConfigService.java` | Business logic + AES/GCM encryption |
| `backend/src/main/java/com/shiftleft/hub/ai/service/EmbeddingService.java` | PGVectorStore integration, auto-embed on publish |
| `backend/src/main/java/com/shiftleft/hub/article/service/ArticleService.java` (modified) | Embedding hook on publishArticle |
| `backend/src/main/java/com/shiftleft/hub/config/SecurityConfig.java` (modified) | /api/ai/** requires authentication |
| `backend/src/main/java/com/shiftleft/hub/common/config/DataSeeder.java` (modified) | Default AiConfig seed + pgvector extension |
| `backend/src/main/resources/application.properties` (modified) | AI config properties added |

## Notable Decisions

- Used `javax.crypto` (not Jakarta) for AES encryption — Java 21 standard library
- Used `ChatClient.Builder` (not `ChatModel.Builder`) — correct Spring AI 2.0 M8 API
- Embedding failure on publish is non-blocking (logged as warning)
- PGVectorStore manages its own internal table structure (no column on Article entity)

## Tasks Completed

1. ✅ Task 1: Spring AI BOM + starters in pom.xml
2. ✅ Task 2: AiConfig entity, repository, package structure, DTOs, SecurityConfig, DataSeeder
3. ✅ Task 3: AiConfigService with AES/GCM encryption + AiConfigController REST API
4. ✅ Task 4: EmbeddingService + Article publish hook + DataSeeder vector extension

## Verification

- `./mvnw compile -q` — BUILD SUCCESS
- Spring AI dependencies resolved
- All admin-only endpoints guarded by `@PreAuthorize("hasRole('ADMIN')")`
- API key encrypted at rest using AES/GCM with SHA-256 derived key
