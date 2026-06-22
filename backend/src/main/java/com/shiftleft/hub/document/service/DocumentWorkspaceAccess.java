package com.shiftleft.hub.document.service;

import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import com.shiftleft.hub.document.domain.Document;
import com.shiftleft.hub.document.domain.DocumentNotFoundException;
import com.shiftleft.hub.document.domain.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Shared lookup for documents scoped to the current workspace.
 *
 * <p>Used by {@link DocumentService}, {@link DocumentConverter}, and the
 * document REST controller to enforce the workspace boundary consistently.
 * Returns the document, or throws {@link DocumentNotFoundException} if the
 * document doesn't exist OR belongs to a different workspace (deliberate —
 * we don't leak the existence of cross-workspace documents).
 */
@Component
@RequiredArgsConstructor
public class DocumentWorkspaceAccess {

    private final DocumentRepository documentRepository;

    /**
     * Loads a document, enforcing the current workspace boundary.
     *
     * @param documentId the document UUID
     * @return the document entity
     * @throws DocumentNotFoundException if not found or not in the current workspace
     */
    public Document requireInCurrentWorkspace(UUID documentId) {
        Document document = documentRepository.findById(documentId)
            .orElseThrow(() -> new DocumentNotFoundException(documentId));
        UUID workspaceId = WorkspaceContextHolder.getCurrentWorkspaceId();
        if (!document.getWorkspaceId().equals(workspaceId)) {
            throw new DocumentNotFoundException(documentId);
        }
        return document;
    }
}
