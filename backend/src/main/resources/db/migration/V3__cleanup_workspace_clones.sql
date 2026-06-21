-- V3: Clean up articles that were cloned into non-public workspaces by the
-- (now-removed) WorkspaceArticleCloner from PR #69.
--
-- The cloner duplicated every public-workspace article into the IT, HR, and
-- Legal workspaces, prefixing the slug with the target workspace's slug
-- (e.g. "it-connect-to-wifi"). It was a startup-only mechanism that
-- contradicted the multi-tenant design from Phase 9. PR #71 removed the
-- cloner; this migration removes the rows it created.
--
-- Safeguards: we only delete an article if a corresponding public-workspace
-- article with the same title and content exists. This protects any article
-- a user may have authored directly in a non-public workspace whose slug
-- happens to start with "it-", "human-resources-", or "legal-".
--
-- Vector store rows for the deleted articles are also removed.
--
-- This migration is idempotent — it is a no-op on databases that never
-- contained cloner output (e.g. a fresh environment spun up after PR #71).

DO $$
DECLARE
    deleted_articles integer;
    deleted_vectors integer;
BEGIN
    DELETE FROM article a
    WHERE a.status = 'PUBLISHED'
      AND a.workspace_id IS NOT NULL
      AND a.slug ~ '^(it|human-resources|legal)-.+$'
      AND EXISTS (
          SELECT 1
          FROM article src
          WHERE src.workspace_id = (
              SELECT id FROM workspace WHERE slug = 'public'
          )
            AND src.title_en = a.title_en
            AND (
              (src.content_en IS NOT NULL AND a.content_en IS NOT NULL AND src.content_en = a.content_en)
              OR (src.content_en IS NULL AND a.content_en IS NULL)
            )
      );

    GET DIAGNOSTICS deleted_articles = ROW_COUNT;

    DELETE FROM vector_store vs
    WHERE vs.metadata ? 'slug'
      AND (vs.metadata->>'slug') ~ '^(it|human-resources|legal)-.+$'
      AND (vs.metadata->>'workspace_id')::uuid IN (
          SELECT id FROM workspace WHERE slug IN ('it', 'human-resources', 'legal')
      )
      AND EXISTS (
          SELECT 1
          FROM vector_store src
          WHERE src.metadata ? 'slug'
            AND (src.metadata->>'workspace_id')::uuid = (
                SELECT id FROM workspace WHERE slug = 'public'
            )
            AND src.content = vs.content
      );

    GET DIAGNOSTICS deleted_vectors = ROW_COUNT;

    RAISE NOTICE 'WorkspaceArticleCloner cleanup: deleted % articles and % vector_store rows',
        deleted_articles, deleted_vectors;
END;
$$;
