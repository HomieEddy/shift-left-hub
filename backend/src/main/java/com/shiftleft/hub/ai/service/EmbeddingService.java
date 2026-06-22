package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import com.shiftleft.hub.common.domain.WorkspaceContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final EmbeddingModelProvider embeddingProvider;
    private final ArticleRepository articleRepository;
    private final AiConfigService aiConfigService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${spring.ai.vectorstore.pgvector.dimensions:768}")
    private int vectorStoreDimensions;

    @Value("${spring.ai.vectorstore.pgvector.schema-name:public}")
    private String vectorStoreSchema;

    /**
     * Number of articles batched into a single {@code vectorStore.add(...)}
     * call. Picked to keep each INSERT small (latency stays low) while
     * cutting the round-trip count by ~50x vs per-article writes.
     */
    private static final int RE_EMBED_BATCH_SIZE = 50;

    /**
     * Stores an embedding for the given article in the vector store.
     *
     * @param article the article to embed and store
     */
    public void storeEmbedding(Article article) {
        vectorStore.add(List.of(toDocument(article)));
    }

    /**
     * Generates and stores an embedding for the given article.
     *
     * @param article the article to process
     */
    public void generateAndStoreEmbedding(Article article) {
        try {
            storeEmbedding(article);
            log.info("Stored embedding for article {}", article.getId());
        } catch (Exception e) {
            log.warn("Failed to store embedding for article {}: {} | rootCause={} | vectorStore={}",
                article.getId(), e.getMessage(), mostSpecificMessage(e), describeVectorStoreSchema());
            log.debug("Embedding storage failure details", e);
        }
    }

    /**
     * Re-embeds all published articles in the vector store, across every workspace.
     *
     * <p>The admin endpoint that triggers this can be invoked while the caller
     * is in any workspace. {@code WorkspaceFilterAspect} (Phase 9) auto-applies
     * a Hibernate {@code workspaceFilter} to every repository call when a
     * workspace context is set, which would otherwise scope this query to a
     * single workspace. We temporarily clear the context for the duration of
     * the lookup so the filter is disabled, then restore the original context.</p>
     */
    public void reEmbedAll() {
        UUID savedWorkspaceId = WorkspaceContextHolder.getCurrentWorkspaceIdOrNull();
        WorkspaceContextHolder.clear();
        List<Article> publishedArticles;
        try {
            var publishedPage = articleRepository.findByStatus(ArticleStatus.PUBLISHED, Pageable.unpaged());
            publishedArticles = publishedPage.getContent();
        } finally {
            if (savedWorkspaceId != null) {
                WorkspaceContextHolder.setCurrentWorkspaceId(savedWorkspaceId);
            }
        }
        log.info("Re-embedding {} published articles across all workspaces", publishedArticles.size());

        List<Document> batch = new ArrayList<>(RE_EMBED_BATCH_SIZE);
        int[] written = {0};
        int[] failed = {0};
        for (Article article : publishedArticles) {
            try {
                batch.add(toDocument(article));
            } catch (Exception e) {
                failed[0]++;
                log.warn("Failed to build embedding for article {}: {} | rootCause={} | vectorStore={}",
                    article.getId(), e.getMessage(), mostSpecificMessage(e), describeVectorStoreSchema());
                log.debug("Embedding build failure details", e);
                continue;
            }
            if (batch.size() >= RE_EMBED_BATCH_SIZE) {
                written[0] += flushBatch(batch);
            }
        }
        if (!batch.isEmpty()) {
            written[0] += flushBatch(batch);
        }
        log.info("Re-embedding complete: {} articles written, {} failed (of {} total)",
            written[0], failed[0], publishedArticles.size());
    }

    /**
     * Builds the {@link Document} for a single article without writing it.
     * Shared by {@link #storeEmbedding(Article)} and the batched re-embed
     * path so the metadata shape stays consistent.
     */
    private Document toDocument(Article article) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("articleId", article.getId().toString());
        metadata.put("workspace_id", article.getWorkspaceId().toString());
        metadata.put("title", article.getTitleEn() != null ? article.getTitleEn() : "");
        metadata.put("slug", article.getSlug() != null ? article.getSlug() : "");
        String content = (article.getContentEn() != null ? article.getContentEn() : "")
            + "\n---\n"
            + (article.getContentFr() != null ? article.getContentFr() : "");
        return new Document(content, metadata);
    }

    /**
     * Writes a batch of documents in one round-trip. Returns the number of
     * documents successfully written; on failure the whole batch is logged
     * and counted as 0 (the caller moves on).
     */
    private int flushBatch(List<Document> batch) {
        int size = batch.size();
        try {
            vectorStore.add(List.copyOf(batch));
            return size;
        } catch (Exception e) {
            log.warn("Failed to write batch of {} embeddings: {} | rootCause={} | vectorStore={}",
                size, e.getMessage(), mostSpecificMessage(e), describeVectorStoreSchema());
            log.debug("Batch write failure details", e);
            return 0;
        } finally {
            batch.clear();
        }
    }

    private String describeVectorStoreSchema() {
        try {
            String embeddingType = jdbcTemplate.queryForObject("""
                SELECT format_type(a.atttypid, a.atttypmod)
                FROM pg_attribute a
                JOIN pg_class c ON c.oid = a.attrelid
                JOIN pg_namespace n ON n.oid = c.relnamespace
                WHERE n.nspname = ?
                  AND c.relname = 'vector_store'
                  AND a.attname = 'embedding'
                  AND NOT a.attisdropped
                """, String.class, vectorStoreSchema);
            return "schema=" + vectorStoreSchema
                + ", configuredDimensions=" + vectorStoreDimensions
                + ", embeddingColumnType=" + embeddingType;
        } catch (Exception e) {
            return "schema=" + vectorStoreSchema
                + ", configuredDimensions=" + vectorStoreDimensions
                + ", inspectionError=" + mostSpecificMessage(e);
        }
    }

    private String mostSpecificMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }
}
