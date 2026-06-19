-- Repair production databases where the original pgvector setup was skipped
-- before V10 was marked applied.
CREATE EXTENSION IF NOT EXISTS vector;

DO $$
DECLARE
    current_embedding_type text;
    target_embedding_type text := 'vector(${embeddingDimensions})';
    vector_type_available boolean;
    primary_key_exists boolean;
BEGIN
    SELECT EXISTS (
        SELECT 1 FROM pg_type t
        JOIN pg_namespace n ON n.oid = t.typnamespace
        WHERE t.typname = 'vector'
          AND n.nspname = 'public'
    )
    INTO vector_type_available;

    IF NOT vector_type_available THEN
        RAISE EXCEPTION 'pgvector extension is not installed; missing public.vector type';
    END IF;

    CREATE TABLE IF NOT EXISTS public.vector_store (
        id UUID NOT NULL,
        content TEXT,
        metadata JSONB,
        embedding vector(${embeddingDimensions})
    );

    ALTER TABLE public.vector_store
        ADD COLUMN IF NOT EXISTS content TEXT;

    ALTER TABLE public.vector_store
        ADD COLUMN IF NOT EXISTS metadata JSONB;

    ALTER TABLE public.vector_store
        ADD COLUMN IF NOT EXISTS embedding vector(${embeddingDimensions});

    SELECT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'pk_vector_store'
          AND conrelid = 'public.vector_store'::regclass
    )
    INTO primary_key_exists;

    IF NOT primary_key_exists THEN
        ALTER TABLE public.vector_store
            ADD CONSTRAINT pk_vector_store PRIMARY KEY (id);
    END IF;

    SELECT format_type(a.atttypid, a.atttypmod)
    INTO current_embedding_type
    FROM pg_attribute a
    JOIN pg_class c ON c.oid = a.attrelid
    JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public'
      AND c.relname = 'vector_store'
      AND a.attname = 'embedding'
      AND NOT a.attisdropped;

    IF current_embedding_type <> target_embedding_type THEN
        DROP INDEX IF EXISTS idx_vector_store_embedding;
        DELETE FROM public.vector_store;
        EXECUTE 'ALTER TABLE public.vector_store '
            || 'ALTER COLUMN embedding TYPE vector(${embeddingDimensions}) USING NULL';
    END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_vector_store_embedding
    ON public.vector_store USING ivfflat (embedding vector_cosine_ops);
