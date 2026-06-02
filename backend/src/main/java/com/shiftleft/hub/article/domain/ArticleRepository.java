package com.shiftleft.hub.article.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

    @Query(value = """
        SELECT id, title_en, title_fr, slug, excerpt, published_at,
               ts_headline('english', content_en, plainto_tsquery('english', :query),
                           'MaxWords=50, MinWords=20, StartSel=<mark>, StopSel=</mark>') AS headline_en,
               ts_headline('french', content_fr, plainto_tsquery('french', :query),
                           'MaxWords=50, MinWords=20, StartSel=<mark>, StopSel=</mark>') AS headline_fr
        FROM article
        WHERE status = 'PUBLISHED'
          AND (tsv_en @@ plainto_tsquery('english', :query)
            OR tsv_fr @@ plainto_tsquery('french', :query))
        ORDER BY ts_rank(tsv_en, plainto_tsquery('english', :query)) DESC
        """,
        countQuery = """
        SELECT count(*)
        FROM article
        WHERE status = 'PUBLISHED'
          AND (tsv_en @@ plainto_tsquery('english', :query)
            OR tsv_fr @@ plainto_tsquery('french', :query))
        """,
        nativeQuery = true)
    Page<Object[]> searchByText(@Param("query") String query, Pageable pageable);
}
