package com.shiftleft.hub.kcs.service;

import com.shiftleft.hub.kcs.domain.KcsDraftingException;
import com.shiftleft.hub.kcs.domain.TicketResolvedEvent;
import com.shiftleft.hub.user.domain.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Async event listener for the KCS drafting pipeline.
 * <p>Listens for {@link TicketResolvedEvent} and triggers AI article synthesis.
 * Runs asynchronously via {@code @Async("kcsTaskExecutor")} so the resolve
 * endpoint returns immediately. Retries up to 3 times with exponential
 * backoff on LLM failures (D-25, D-26).</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class KcsEventListener {

    private final KcsDraftingService draftingService;
    private final KcsSupportService kcsSupport;

    private static final int MAX_RETRIES = 3;
    private static final long BASE_BACKOFF_MS = 1000;

    /**
     * Processes a TicketResolvedEvent asynchronously.
     * Uses @TransactionalEventListener to ensure the event fires AFTER
     * the resolve transaction commits, preventing race conditions.
     *
     * @param event the resolved ticket event
     */
    @Async("kcsTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleTicketResolved(TicketResolvedEvent event) {
        log.info("KCS listener received TicketResolvedEvent for ticket {}",
            event.ticketNumber());

        try {
            // Each helper opens its own short transaction via KcsSupportService.
            // Crucially, the outer method is NOT @Transactional — the retry
            // loop's Thread.sleep must not hold a DB connection for up to
            // 7s (1s + 2s + 4s). See P-9.
            User systemUser = kcsSupport.getOrCreateSystemUser();

            // Attempt drafting with retries (D-25)
            var article = draftWithRetry(event, systemUser);

            // On success: add auto-generated work note to source ticket (D-23)
            kcsSupport.recordWorkNote(event.ticketId(), systemUser, article.getId());

        } catch (KcsDraftingException e) {
            // Non-retryable error — log with full context (D-27)
            log.error("KCS drafting failed for ticket {} (non-retryable): {}",
                event.ticketNumber(), e.getMessage());
        } catch (Exception e) {
            // Unexpected error after retries exhausted (D-26)
            log.error("KCS drafting exhausted retries for ticket {}: {}",
                event.ticketNumber(), e.getMessage(), e);
        }
    }

    /**
     * Attempts LLM-based drafting with exponential backoff retries.
     * Uses an iterative loop instead of recursion to avoid blocking
     * the limited async thread pool with recursive call chains.
     * Only retries on LLM-related failures (timeout, generation error).
     * Non-LLM errors (DB constraint, invalid state) throw KcsDraftingException immediately.
     */
    private com.shiftleft.hub.article.domain.Article draftWithRetry(
            TicketResolvedEvent event, User systemUser) {
        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                return draftingService.draftArticle(event, systemUser);
            } catch (Exception e) {
                boolean isRetryable = isLikelyLlmError(e);
                if (!isRetryable) {
                    throw new KcsDraftingException(
                        "Non-retryable KCS drafting error: " + e.getMessage(), e);
                }
                if (attempt < MAX_RETRIES) {
                    long backoff = BASE_BACKOFF_MS * (long) Math.pow(2, attempt - 1);
                    log.warn("KCS draft attempt {} failed for ticket {}, retrying in {}ms: {}",
                        attempt, event.ticketNumber(), backoff, e.getMessage());
                    try {
                        Thread.sleep(backoff);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new KcsDraftingException("Retry interrupted", ie);
                    }
                } else {
                    throw new RuntimeException(
                        "KCS drafting failed after " + MAX_RETRIES + " attempts", e);
                }
            }
        }
        // Unreachable: the loop body returns on success, throws on
        // failure. Java's flow analysis still requires a return path.
        throw new IllegalStateException("KCS drafting reached an unreachable state");
    }

    /** Determines if an exception is likely LLM-related and retryable. */
    private boolean isLikelyLlmError(Throwable e) {
        // Spring AI transient exceptions are always retryable
        if (e instanceof org.springframework.ai.retry.TransientAiException) {
            return true;
        }
        // Non-transient AI exceptions are not retryable
        if (e instanceof org.springframework.ai.retry.NonTransientAiException) {
            return false;
        }
        // Fallback: check message for LLM-specific error patterns
        String msg = e.getMessage() != null ? e.getMessage().toLowerCase() : "";
        return msg.contains("timeout")
            || msg.contains("too many requests")
            || msg.contains("rate limit")
            || msg.contains("server error");
    }
}
