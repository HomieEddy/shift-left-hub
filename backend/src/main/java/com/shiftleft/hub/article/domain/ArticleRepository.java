package com.shiftleft.hub.article.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing {@link Article} entities.
 */
public interface ArticleRepository extends JpaRepository<Article, UUID> {

    /**
     * Finds an article by its slug.
     *
     * @param slug the article slug
     * @return an Optional containing the article if found
     */
    Optional<Article> findBySlug(String slug);

    Optional<Article> findByIdAndWorkspaceId(UUID id, UUID workspaceId);

    /**
     * Finds articles by their status.
     *
     * @param status   the article status to filter by
     * @param pageable the pagination information
     * @return a page of articles with the given status
     */
    Page<Article> findByStatus(ArticleStatus status, Pageable pageable);

    /**
     * Page of articles by status with the {@code tags} collection
     * eagerly loaded. Used by the admin listing endpoint, which maps
     * every article to a DTO that reads the tags; a plain
     * {@code findByStatus} would fire one SELECT per row for the
     * lazy {@code tags} collection. {@code author} and
     * {@code lastEditor} are already eager and need no graph entry.
     *
     * @param status the article status to filter by
     * @param pageable the page request
     * @return the page of articles with tags initialized
     */
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"tags"})
    Page<Article> findWithAssociationsByStatus(ArticleStatus status, Pageable pageable);

    Page<Article> findByStatusAndWorkspaceId(ArticleStatus status, UUID workspaceId, Pageable pageable);

    /**
     * Full-text search across published articles.
     *
     * @param query    the search query
     * @param pageable the pagination information
     * @return a page of raw search result rows
     */
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

    /**
     * Full-text search across published articles scoped to a workspace.
     *
     * <p>CR-01 assessment: Native SQL bypasses Hibernate @Filter but workspace_id is
     * explicitly filtered via parameter binding (CAST(:workspaceId AS UUID)).
     * This is deliberate — ts_headline/ts_rank functions are not available in JPQL.
     * The workspaceId always comes from authenticated JWT context, preventing
     * cross-tenant data leakage.
     *
     * @param query       the search query
     * @param workspaceId the workspace UUID to scope results to
     * @param pageable    the pagination information
     * @return a page of raw search result rows
     */
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
          AND a.workspace_id = CAST(:workspaceId AS UUID)
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
          AND a.workspace_id = CAST(:workspaceId AS UUID)
        """,
        nativeQuery = true)
    Page<Object[]> searchByText(
            @Param("query") String query,
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable);

    /**
     * Returns only the IDs of published articles matching the FTS query,
     * in rank order. Used by KcsDraftingService.checkDuplicates, which
     * doesn't need title/slug/excerpt/headline/tag data. Skips the
     * tag_names correlated subquery that the full projection pays for.
     *
     * @param query the user's search text
     * @param workspaceId the workspace to scope the search to
     * @param pageable the page request
     * @return a page of article ids
     */
    @Query(value = """
        WITH q AS (
          SELECT plainto_tsquery('english', :query) AS en_query,
                 plainto_tsquery('french', :query) AS fr_query
        )
        SELECT a.id
        FROM article a
        CROSS JOIN q
        WHERE a.workspace_id = CAST(:workspaceId AS UUID)
          AND a.status = 'PUBLISHED'
          AND (a.tsv_en @@ q.en_query OR a.tsv_fr @@ q.fr_query)
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
        WHERE a.workspace_id = CAST(:workspaceId AS UUID)
          AND a.status = 'PUBLISHED'
          AND (a.tsv_en @@ q.en_query OR a.tsv_fr @@ q.fr_query)
        """,
        nativeQuery = true)
    Page<UUID> searchIdsByText(@Param("query") String query,
        @Param("workspaceId") UUID workspaceId, Pageable pageable);

    /**
     * Full-text search across published articles filtered by tag names.
     *
     * @param query    the search query
     * @param tagNames the tag names to filter by
     * @param pageable the pagination information
     * @return a page of raw search result rows
     */
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
    Page<Object[]> searchByTextAndTagNames(
            @Param("query") String query,
            @Param("tagNames") Collection<String> tagNames,
            Pageable pageable);

    /**
     * Full-text search across published articles scoped to a workspace, filtered by tag names.
     *
     * @param query       the search query
     * @param tagNames    the tag names to filter by
     * @param workspaceId the workspace UUID to scope results to
     * @param pageable    the pagination information
     * @return a page of raw search result rows
     */
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
          AND a.workspace_id = CAST(:workspaceId AS UUID)
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
          AND a.workspace_id = CAST(:workspaceId AS UUID)
        """,
        nativeQuery = true)
    Page<Object[]> searchByTextAndTagNames(
            @Param("query") String query,
            @Param("tagNames") Collection<String> tagNames,
            @Param("workspaceId") UUID workspaceId,
            Pageable pageable);

    /**
     * Finds tag facet counts for published articles.
     *
     * @return a list of raw tag facet rows
     */
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

    /**
     * Finds tag facet counts for published articles scoped to a workspace.
     *
     * @param workspaceId the workspace UUID to scope results to
     * @return a list of raw tag facet rows
     */
    @Query(value = """
        SELECT t.name_en, t.name_fr, t.color, COUNT(*)
        FROM article a
        JOIN article_tag at ON at.article_id = a.id
        JOIN tag t ON t.id = at.tag_id
        WHERE a.status = 'PUBLISHED'
          AND a.workspace_id = CAST(:workspaceId AS UUID)
        GROUP BY t.id, t.name_en, t.name_fr, t.color
        ORDER BY t.name_en
        """, nativeQuery = true)
    List<Object[]> findPublishedTagFacets(@Param("workspaceId") UUID workspaceId);

    // === KCS Draft Query Methods ===

    /**
     * Finds articles that originated from tickets, ordered by creation date descending.
     *
     * @param pageable the pagination information
     * @return a page of KCS draft articles
     */
    Page<Article> findBySourceTicketIdIsNotNullOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Finds an article by its source ticket ID.
     *
     * @param sourceTicketId the source ticket UUID
     * @return an Optional containing the article if found
     */
    Optional<Article> findBySourceTicketId(UUID sourceTicketId);

    List<Article> findByCategoryId(UUID categoryId);

    /**
     * Counts articles with a given status.
     *
     * @param status the article status to count
     * @return the count of matching articles
     */
    long countByStatus(ArticleStatus status);

    /**
     * Counts articles assigned to a given category.
     *
     * @param categoryId the category UUID
     * @return number of articles in the category
     */
    long countByCategoryId(UUID categoryId);

    /**
     * Bulk-reassigns all articles from one category to another.
     *
     * @param sourceId   the source category UUID
     * @param categoryId the target category UUID
     * @return number of articles updated
     */
    @Modifying
    @Query("UPDATE Article a SET a.category.id = :categoryId WHERE a.category.id = :sourceId")
    int reassignCategory(@Param("sourceId") UUID sourceId, @Param("categoryId") UUID categoryId);

    /**
     * Counts articles that originated from tickets by their status.
     *
     * @param status the article status to count
     * @return the count of matching articles
     */
    long countBySourceTicketIdIsNotNullAndStatus(ArticleStatus status);

    /**
     * Counts articles associated with a given tag.
     *
     * @param tagId the tag UUID
     * @return the number of articles tagged with the given tag
     */
    @Query("SELECT COUNT(a) FROM Article a JOIN a.tags t WHERE t.id = :tagId")
    long countByTagId(@Param("tagId") UUID tagId);

    /**
     * Returns the number of articles per tag id, for the given set of
     * tag ids, in a single query. Used by the tag listing endpoint to
     * avoid firing one COUNT query per tag.
     *
     * @param tagIds the tag ids to count
     * @return a list of [tagId, articleCount] tuples
     */
    @Query("SELECT t.id, COUNT(a) FROM Article a JOIN a.tags t "
        + "WHERE t.id IN :tagIds GROUP BY t.id")
    List<Object[]> countByTagIds(@Param("tagIds") java.util.Collection<UUID> tagIds);
}
