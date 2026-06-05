package com.shiftleft.hub.kcs.domain;

/**
 * Exception thrown when KCS auto-drafting encounters a non-retryable error.
 * <p>Non-LLM errors (DB constraint, invalid state) should use this.
 * LLM failures are retried — see {@link com.shiftleft.hub.kcs.service.KcsEventListener}.</p>
 */
public class KcsDraftingException extends RuntimeException {
    public KcsDraftingException(String message) {
        super(message);
    }
    public KcsDraftingException(String message, Throwable cause) {
        super(message, cause);
    }
}
