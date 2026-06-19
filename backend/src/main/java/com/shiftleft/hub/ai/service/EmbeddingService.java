package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * Generates an embedding vector for the given text.
     *
     * @param text the input text
     * @return the embedding vector
     */
    public float[] generateEmbedding(String text) {
        return embeddingProvider.getEmbeddingModel().embed(text);
    }

    /**
     * Stores an embedding for the given article in the vector store.
     *
     * @param article the article to embed and store
     */
    public void storeEmbedding(Article article) {
        String content = (article.getContentEn() != null ? article.getContentEn() : "")
            + "\n---\n"
            + (article.getContentFr() != null ? article.getContentFr() : "");

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("articleId", article.getId().toString());
        metadata.put("title", article.getTitleEn() != null ? article.getTitleEn() : "");
        metadata.put("slug", article.getSlug() != null ? article.getSlug() : "");
        Document document = new Document(content, metadata);
        vectorStore.add(List.of(document));
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
     * Re-embeds all published articles in the vector store.
     */
    public void reEmbedAll() {
        var publishedPage = articleRepository.findByStatus(ArticleStatus.PUBLISHED, Pageable.unpaged());
        List<Article> publishedArticles = publishedPage.getContent();
        log.info("Re-embedding {} published articles", publishedArticles.size());

        for (Article article : publishedArticles) {
            try {
                generateAndStoreEmbedding(article);
            } catch (Exception e) {
                log.warn("Failed to re-embed article {}: {} | rootCause={} | vectorStore={}",
                    article.getId(), e.getMessage(), mostSpecificMessage(e), describeVectorStoreSchema());
                log.debug("Re-embedding failure details", e);
            }
        }
        log.info("Re-embedding complete for {} articles", publishedArticles.size());
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
