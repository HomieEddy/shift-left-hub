-- Keep Spring AI's vector_store table aligned with the configured embedding model.
-- Existing embeddings are deleted because vectors from one dimension cannot be
-- converted safely into another dimension.
DO $$
DECLARE
    current_embedding_type text;
    target_embedding_type text := 'vector(${embeddingDimensions})';
BEGIN
    SELECT format_type(a.atttypid, a.atttypmod)
    INTO current_embedding_type
    FROM pg_attribute a
    JOIN pg_class c ON c.oid = a.attrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname = 'vector_store'
      AND a.attname = 'embedding'
      AND NOT a.attisdropped;

    IF current_embedding_type IS NULL THEN
        RAISE EXCEPTION 'vector_store.embedding column was not found';
    END IF;

    IF current_embedding_type <> target_embedding_type THEN
        DROP INDEX IF EXISTS idx_vector_store_embedding;
        DELETE FROM vector_store;
        EXECUTE 'ALTER TABLE public.vector_store '
            || 'ALTER COLUMN embedding TYPE vector(${embeddingDimensions}) USING NULL';
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON vector_store USING ivfflat (embedding vector_cosine_ops);
