package com.shiftleft.hub.article.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ArticleRepository extends JpaRepository<Article, UUID> {

    java.util.Optional<Article> findBySlug(String slug);

    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

    @Query(value = """
        WITH q AS (
          SELECT plainto_tsquery('english', :query) AS en_query,
                 plainto_tsquery('french', :query) AS fr_query
        )
        SELECT a.id, a.title_en, a.title_fr, a.slug, a.excerpt, a.published_at,
               ts_headline('english', a.content_en, q.en_query,
                           'MaxWords=50, MinWords=20, StartSel=<mark>, StopSel=</mark>') AS headline_en,
               ts_headline('french', a.content_fr, q.fr_query,
                           'MaxWords=50, MinWords=20, StartSel=<mark>, StopSel=</mark>') AS headline_fr,
               COALESCE(
                 (
                   SELECT array_agg(t.name_en ORDER BY t.name_en)
                   FROM article_tag at2
                   JOIN tag t ON t.id = at2.tag_id
                   WHERE at2.article_id = a.id
                 ),
                 ARRAY[]::text[]
               ) AS tag_names
        FROM article a
        CROSS JOIN q
        WHERE a.status = 'PUBLISHED'
          AND (a.tsv_en @@ q.en_query
            OR a.tsv_fr @@ q.fr_query)
        ORDER BY GREATEST(ts_rank(a.tsv_en, q.en_query), ts_rank(a.tsv_fr, q.fr_query)) DESC
        """,
        countQuery = """
        WITH q AS (
          SELECT plainto_tsquery('english', :query) AS en_query,
                 plainto_tsquery('french', :query) AS fr_query
        )
        SELECT count(*)
        FROM article a
        CROSS JOIN q
        WHERE a.status = 'PUBLISHED'
          AND (a.tsv_en @@ q.en_query
            OR a.tsv_fr @@ q.fr_query)
        """,
        nativeQuery = true)
    Page<Object[]> searchByText(@Param("query") String query, Pageable pageable);

    @Query(value = """
        WITH q AS (
          SELECT plainto_tsquery('english', :query) AS en_query,
                 plainto_tsquery('french', :query) AS fr_query
        )
        SELECT a.id, a.title_en, a.title_fr, a.slug, a.excerpt, a.published_at,
               ts_headline('english', a.content_en, q.en_query,
                           'MaxWords=50, MinWords=20, StartSel=<mark>, StopSel=</mark>') AS headline_en,
               ts_headline('french', a.content_fr, q.fr_query,
                           'MaxWords=50, MinWords=20, StartSel=<mark>, StopSel=</mark>') AS headline_fr,
               COALESCE(
                 (
                   SELECT array_agg(t.name_en ORDER BY t.name_en)
                   FROM article_tag at2
                   JOIN tag t ON t.id = at2.tag_id
                   WHERE at2.article_id = a.id
                 ),
                 ARRAY[]::text[]
               ) AS tag_names
        FROM article a
        CROSS JOIN q
        WHERE a.status = 'PUBLISHED'
          AND (a.tsv_en @@ q.en_query
            OR a.tsv_fr @@ q.fr_query)
          AND EXISTS (
            SELECT 1
            FROM article_tag atf
            JOIN tag tf ON tf.id = atf.tag_id
            WHERE atf.article_id = a.id
              AND tf.name_en IN (:tagNames)
          )
        ORDER BY GREATEST(ts_rank(a.tsv_en, q.en_query), ts_rank(a.tsv_fr, q.fr_query)) DESC
        """,
        countQuery = """
        WITH q AS (
          SELECT plainto_tsquery('english', :query) AS en_query,
                 plainto_tsquery('french', :query) AS fr_query
        )
        SELECT count(*)
        FROM article a
        CROSS JOIN q
        WHERE a.status = 'PUBLISHED'
          AND (a.tsv_en @@ q.en_query
            OR a.tsv_fr @@ q.fr_query)
          AND EXISTS (
            SELECT 1
            FROM article_tag atf
            JOIN tag tf ON tf.id = atf.tag_id
            WHERE atf.article_id = a.id
              AND tf.name_en IN (:tagNames)
          )
        """,
        nativeQuery = true)
    Page<Object[]> searchByTextAndTagNames(@Param("query") String query, @Param("tagNames") Collection<String> tagNames, Pageable pageable);

    @Query(value = """
        SELECT t.name_en, t.name_fr, t.color, COUNT(*)
        FROM article a
        JOIN article_tag at ON at.article_id = a.id
        JOIN tag t ON t.id = at.tag_id
        WHERE a.status = 'PUBLISHED'
        GROUP BY t.id, t.name_en, t.name_fr, t.color
        ORDER BY t.name_en
        """, nativeQuery = true)
    List<Object[]> findPublishedTagFacets();

    // === KCS Draft Query Methods ===

    Page<Article> findBySourceTicketIdIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    Optional<Article> findBySourceTicketId(UUID sourceTicketId);

    long countBySourceTicketIdIsNotNullAndStatus(ArticleStatus status);
}
