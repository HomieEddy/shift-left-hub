package com.shiftleft.hub.document.service;

import com.shiftleft.hub.document.domain.DocumentChunk;
import com.shiftleft.hub.document.domain.DocumentChunkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentEmbeddingService {

    private final EmbeddingModel embeddingModel;
    private final DocumentChunkRepository documentChunkRepository;

    /**
     * Generates vector embeddings for document chunks using the configured embedding model.
     * Updates each chunk entity with its computed embedding vector.
     *
     * @param documentId    the document UUID
     * @param chunkContents the list of chunk text contents
     */
    public void generateEmbeddings(UUID documentId, List<String> chunkContents) {
        if (chunkContents.isEmpty()) {
            log.warn("No chunks to embed for document {}", documentId);
            return;
        }

        // Generate embeddings in batch
        List<List<Double>> embeddings = embeddingModel.embed(chunkContents).stream()
            .map(fa -> {
                List<Double> list = new ArrayList<>();
                for (float f : fa) {
                    list.add((double) f);
                }
                return list;
            })
            .toList();

        // Update each chunk with its embedding
        List<DocumentChunk> chunks = documentChunkRepository.findByDocumentIdOrderByChunkIndexAsc(documentId);
        for (int i = 0; i < Math.min(chunks.size(), embeddings.size()); i++) {
            List<Double> embedding = embeddings.get(i);
            float[] floatEmbedding = new float[embedding.size()];
            for (int j = 0; j < embedding.size(); j++) {
                floatEmbedding[j] = embedding.get(j).floatValue();
            }
            chunks.get(i).setEmbedding(floatEmbedding);
        }
        documentChunkRepository.saveAll(chunks);

        log.info("Generated embeddings for {} chunks of document {}", chunkContents.size(), documentId);
    }
}
