package com.shiftleft.hub.ai.api;

import com.shiftleft.hub.ai.api.dto.ChatRequest;
import com.shiftleft.hub.ai.api.dto.StreamEvent;
import com.shiftleft.hub.ai.service.AiChatService;
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
import java.util.concurrent.ExecutorService;

@RestController
@RequestMapping("/api/ai")
@Slf4j
public class ChatController {

    private final AiChatService aiChatService;
    private final ExecutorService chatExecutor;

    public ChatController(AiChatService aiChatService, @Qualifier("chatExecutor") ExecutorService chatExecutor) {
        this.aiChatService = aiChatService;
        this.chatExecutor = chatExecutor;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chat(@Valid @RequestBody ChatRequest request, Authentication auth) {
        SseEmitter emitter = new SseEmitter(30_000L);

        chatExecutor.submit(() -> {
            try {
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
