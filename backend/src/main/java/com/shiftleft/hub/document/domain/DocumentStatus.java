package com.shiftleft.hub.document.domain;

/**
 * Processing status for document ingestion pipeline.
 * Tracks the 5-stage processing lifecycle plus a FAILED terminal state.
 */
public enum DocumentStatus {
    UPLOADED,
    PARSING,
    CHUNKING,
    EMBEDDING,
    READY,
    FAILED
}
