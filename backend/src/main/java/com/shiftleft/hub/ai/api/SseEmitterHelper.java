package com.shiftleft.hub.ai.api;

import com.shiftleft.hub.ai.api.dto.StreamEvent;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * Shared helper for the "send one SSE event" pattern that appears in every
 * SSE-emitting controller and service in this module. Centralising it keeps
 * the IOException/Complete-with-error handling consistent and lets the call
 * sites read like a single intent.
 */
public final class SseEmitterHelper {

    private SseEmitterHelper() {
    }

    /**
     * Sends a single SSE event without completing the emitter. Returns false
     * if the send failed (typically because the client disconnected); the
     * emitter is already completed-with-error in that case and the caller
     * should stop sending further events.
     *
     * @param emitter the SSE emitter to write to
     * @param event the event payload
     * @return true if the send succeeded; false if the client disconnected
     */
    public static boolean tryEmit(SseEmitter emitter, StreamEvent event) {
        try {
            emitter.send(SseEmitter.event().name("message").data(event));
            return true;
        } catch (IOException e) {
            emitter.completeWithError(e);
            return false;
        }
    }

    /**
     * Sends a single SSE event and completes the emitter.
     *
     * @param emitter the SSE emitter to write to
     * @param event the event payload
     */
    public static void emitAndComplete(SseEmitter emitter, StreamEvent event) {
        try {
            emitter.send(SseEmitter.event().name("message").data(event));
            emitter.complete();
        } catch (IOException e) {
            emitter.completeWithError(e);
        }
    }

    /**
     * Sends an error event and completes the emitter.
     *
     * @param emitter the SSE emitter to write to
     * @param message the error message
     */
    public static void emitErrorAndComplete(SseEmitter emitter, String message) {
        emitAndComplete(emitter, new StreamEvent("error", message, null));
    }
}

