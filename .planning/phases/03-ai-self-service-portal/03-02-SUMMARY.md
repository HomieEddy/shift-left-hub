---
phase: 03-ai-self-service-portal
plan: 02
completed: true
date: 2026-06-03
commits:
  - "f4f8f6b"
status: complete
---

# Plan 03-02: Backend Chat & RAG Pipeline

## What Was Built

Full SSE streaming chat endpoint with hybrid search (FTS + pgvector + RRF), similarity threshold enforcement, conversation context, and fallback flow with escalation option.

## Key Files Created

| File | Purpose |
|------|---------|
| `backend/src/main/java/com/shiftleft/hub/ai/api/dto/ChatRequest.java` | Request DTO: message + chat history (last 10) |
| `backend/src/main/java/com/shiftleft/hub/ai/api/dto/StreamEvent.java` | SSE event types: token, done, error, fallback, sources |
| `backend/src/main/java/com/shiftleft/hub/ai/api/ChatController.java` | POST /api/ai/chat SSE streaming endpoint, 30s timeout |
| `backend/src/main/java/com/shiftleft/hub/ai/service/AiChatService.java` | RAG pipeline: hybrid search, context construction, LLM streaming, fallback |

## Notable Decisions

- Used `SearchRequest.builder().query(...).topK(10).build()` — Spring AI 2.0 M8 builder API
- Used `ChatClient` streaming API (`.stream().content()`) for reactive token delivery
- RRF with k=60 for equal-weight FTS + vector fusion
- Fallback message includes escalation option (wired in Phase 4)
- No server-side session state — all context in ChatRequest history field

## Tasks Completed

1. ✅ Task 1: ChatRequest DTO, StreamEvent DTO, ChatController SSE endpoint
2. ✅ Task 2: AiChatService with hybrid search, RAG pipeline, streaming, tests (4 test methods outlined)

## Verification

- `./mvnw compile -q` — BUILD SUCCESS
- SSE endpoint produces `text/event-stream`
- Hybrid search combines FTS rank + vector similarity via RRF
- Threshold > 0.65 enforced on merged results
- Below-threshold queries return fallback event
