package com.shiftleft.hub.document.domain;

public class DuplicateDocumentException extends RuntimeException {
    private final String existingFilename;
    private final String contentHash;

    public DuplicateDocumentException(String existingFilename, String contentHash) {
        super("Document content already exists as: " + existingFilename);
        this.existingFilename = existingFilename;
        this.contentHash = contentHash;
    }

    public String getExistingFilename() {
        return existingFilename;
    }

    public String getContentHash() {
        return contentHash;
    }
}
