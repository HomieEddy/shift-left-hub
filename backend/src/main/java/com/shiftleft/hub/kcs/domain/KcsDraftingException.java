package com.shiftleft.hub.kcs.domain;

/**
 * Exception thrown when KCS auto-drafting encounters a non-retryable error.
 * <p>Non-LLM errors (DB constraint, invalid state) should use this.
 * LLM failures are retried — see {@link com.shiftleft.hub.kcs.service.KcsEventListener}.</p>
 */
public class KcsDraftingException extends RuntimeException {

    /**
     * Creates a new exception with a message.
     *
     * @param message the error description
     */
    public KcsDraftingException(String message) {
        super(message);
    }

    /**
     * Creates a new exception with a message and cause.
     *
     * @param message the error description
     * @param cause   the root cause
     */
    public KcsDraftingException(String message, Throwable cause) {
        super(message, cause);
    }
}
