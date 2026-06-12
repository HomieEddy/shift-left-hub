package com.shiftleft.hub.document.domain;

public class DocumentProcessingException extends RuntimeException {
    public DocumentProcessingException(String message, Throwable cause) {
        super(message, cause);
    }

    public DocumentProcessingException(String message) {
        super(message);
    }
}
