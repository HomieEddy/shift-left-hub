package com.shiftleft.hub.ai.service;

import com.shiftleft.hub.article.domain.Article;
import com.shiftleft.hub.article.domain.ArticleRepository;
import com.shiftleft.hub.article.domain.ArticleStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class EmbeddingService {

    private final VectorStore vectorStore;
    private final EmbeddingModel embeddingModel;
    private final ArticleRepository articleRepository;
    private final AiConfigService aiConfigService;

    public float[] generateEmbedding(String text) {
        return embeddingModel.embed(text);
    }

    public void storeEmbedding(Article article) {
        String content = (article.getContentEn() != null ? article.getContentEn() : "")
            + "\n---\n"
            + (article.getContentFr() != null ? article.getContentFr() : "");

        Document document = new Document(
            content,
            Map.of(
                "articleId", article.getId().toString(),
                "title", article.getTitleEn(),
                "slug", article.getSlug()
            )
        );
        vectorStore.add(List.of(document));
    }

    public void generateAndStoreEmbedding(Article article) {
        try {
            storeEmbedding(article);
            log.info("Stored embedding for article {}", article.getId());
        } catch (Exception e) {
            log.warn("Failed to store embedding for article {}: {}", article.getId(), e.getMessage());
        }
    }

    public void reEmbedAll() {
        List<Article> publishedArticles = articleRepository.findByStatus(ArticleStatus.PUBLISHED, Pageable.unpaged()).getContent();
        log.info("Re-embedding {} published articles", publishedArticles.size());

        for (Article article : publishedArticles) {
            try {
                generateAndStoreEmbedding(article);
            } catch (Exception e) {
                log.warn("Failed to re-embed article {}: {}", article.getId(), e.getMessage());
            }
        }
        log.info("Re-embedding complete for {} articles", publishedArticles.size());
    }
}
