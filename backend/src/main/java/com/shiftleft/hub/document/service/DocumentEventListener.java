package com.shiftleft.hub.document.service;

import com.shiftleft.hub.document.domain.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Paths;
import java.util.List;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentEventListener {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository documentChunkRepository;
    private final DocumentParserService documentParserService;
    private final DocumentChunkingService documentChunkingService;
    private final DocumentEmbeddingService documentEmbeddingService;

    /**
     * Handles the document uploaded event by processing the document through the
     * ETL pipeline: parse → chunk → embed → mark READY.
     * Runs asynchronously after document upload completes.
     *
     * @param event the document uploaded event containing document and workspace IDs
     */
    @EventListener
    @Async
    @Transactional
    public void handleDocumentUploaded(DocumentUploadedEvent event) {
        log.info("ETL stage: PARSING document {}", event.documentId());
        try {
            Document document = documentRepository.findById(event.documentId())
                .orElseThrow(() -> new DocumentNotFoundException(event.documentId()));

            document.setStatus(DocumentStatus.PARSING);
            documentRepository.save(document);

            String content = documentParserService.parse(
                Paths.get(document.getFilePath()),
                document.getMimeType()
            );

            document.setStatus(DocumentStatus.CHUNKING);
            documentRepository.save(document);

            // Continue to chunking
            List<String> chunks = documentChunkingService.chunk(content);

            // Save chunks in batch
            List<DocumentChunk> entities = IntStream.range(0, chunks.size())
                .mapToObj(i -> DocumentChunk.builder()
                    .documentId(event.documentId())
                    .content(chunks.get(i))
                    .chunkIndex(i)
                    .build())
                .toList();
            documentChunkRepository.saveAll(entities);

            document.setStatus(DocumentStatus.EMBEDDING);
            document.setChunkCount(chunks.size());
            documentRepository.save(document);

            // Generate embeddings
            documentEmbeddingService.generateEmbeddings(event.documentId(), chunks);

            // Mark as ready
            document.setStatus(DocumentStatus.READY);
            documentRepository.save(document);

            log.info("ETL pipeline complete for document {} — {} chunks embedded", event.documentId(), chunks.size());
        } catch (Exception e) {
            log.error("ETL pipeline failed for document {}: {}", event.documentId(), e.getMessage(), e);
            Document document = documentRepository.findById(event.documentId()).orElse(null);
            if (document != null) {
                document.setStatus(DocumentStatus.FAILED);
                document.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Unknown processing error");
                documentRepository.save(document);
            }
        }
    }
}
