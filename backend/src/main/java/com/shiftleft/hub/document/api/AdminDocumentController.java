package com.shiftleft.hub.document.api;

import com.shiftleft.hub.document.api.dto.DocumentListResponse;
import com.shiftleft.hub.document.api.dto.DocumentUploadResponse;
import com.shiftleft.hub.document.domain.Document;
import com.shiftleft.hub.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/documents")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDocumentController {

    private final DocumentService documentService;

    /**
     * Uploads a document file. Starts the async ETL pipeline for processing.
     *
     * @param file the uploaded multipart file
     * @return the upload response with document metadata
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentUploadResponse> uploadDocument(@RequestParam("file") MultipartFile file) {
        Document document = documentService.uploadDocument(file);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(DocumentUploadResponse.from(document));
    }

    /**
     * Lists all documents for the current workspace.
     *
     * @return list of document summaries
     */
    @GetMapping
    public ResponseEntity<List<DocumentListResponse>> listDocuments() {
        List<Document> documents = documentService.listDocuments();
        List<DocumentListResponse> response = documents.stream()
            .map(DocumentListResponse::from)
            .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Returns a single document by ID.
     *
     * @param id the document UUID
     * @return the document details
     */
    @GetMapping("/{id}")
    public ResponseEntity<DocumentListResponse> getDocument(@PathVariable UUID id) {
        Document document = documentService.getDocument(id);
        return ResponseEntity.ok(DocumentListResponse.from(document));
    }

    /**
     * Deletes a document and its associated chunks and file.
     *
     * @param id the document UUID to delete
     * @return 204 No Content on success
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Reprocesses a document by resetting its status and restarting the ETL pipeline.
     *
     * @param id the document UUID to reprocess
     * @return the reprocessed document metadata
     */
    @PostMapping("/{id}/reprocess")
    public ResponseEntity<DocumentUploadResponse> reprocessDocument(@PathVariable UUID id) {
        Document document = documentService.reprocessDocument(id);
        return ResponseEntity.ok(DocumentUploadResponse.from(document));
    }
}
