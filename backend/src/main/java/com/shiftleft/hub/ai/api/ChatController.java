package com.shiftleft.hub.ai.api;

import com.shiftleft.hub.ai.api.dto.ChatRequest;
import com.shiftleft.hub.ai.api.dto.StreamEvent;
import com.shiftleft.hub.ai.service.AiChatService;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/ai")
@Slf4j
public class ChatController {

    private final AiChatService aiChatService;
    private final ExecutorService chatExecutor;

    /**
     * Constructs a ChatController with the given chat service and executor.
     *
     * @param aiChatService the AI chat service
     * @param chatExecutor  the executor for async chat processing
     */
    public ChatController(AiChatService aiChatService, @Qualifier("chatExecutor") ExecutorService chatExecutor) {
        this.aiChatService = aiChatService;
        this.chatExecutor = chatExecutor;
    }

    /**
     * Processes a chat request and streams the response as SSE events.
     *
     * @param request the chat request with message and optional history
     * @param auth    the authenticated principal
     * @return SSE emitter for streaming the response
     */
    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest request, Authentication auth) {
        SseEmitter emitter = new SseEmitter(30_000L);
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();

        chatExecutor.submit(() -> {
            try {
                if (workspaceId != null) {
                    WorkspaceContextHolder.setCurrentWorkspaceId(workspaceId);
                }
                aiChatService.processChat(request, emitter, auth.getName());
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event()
                        .name("message")
                        .data(new StreamEvent("error", "An error occurred: " + e.getMessage(), null)));
                    emitter.complete();
                } catch (IOException ex) {
                    emitter.completeWithError(ex);
                }
            } finally {
                if (workspaceId != null) {
                    WorkspaceContextHolder.clear();
                }
            }
        });

        emitter.onTimeout(() -> {
            try {
                emitter.send(SseEmitter.event()
                    .name("message")
                    .data(new StreamEvent("error", "Request timed out after 30 seconds", null)));
            } catch (IOException e) {
                log.warn("Failed to send timeout error to client", e);
            }
            emitter.complete();
        });

        return emitter;
    }
}
