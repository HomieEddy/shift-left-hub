package com.shiftleft.hub.ai.api.dto;

/**
 * Result of a connection test against an AI provider.
 *
 * @param success whether the connection was successful
 * @param message a human-readable result message
 */
public record TestConnectionResult(
    boolean success,
    String message
) {
}
