-- V9: Ensure tsv_content column and trigger exist on document_chunk
-- (idempotent re-apply of V5 additions in case migration was skipped)

ALTER TABLE document_chunk ADD COLUMN IF NOT EXISTS tsv_content TSVECTOR;

CREATE OR REPLACE FUNCTION update_document_chunk_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tsv_content := to_tsvector('english', COALESCE(NEW.content, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_document_chunk_tsv ON document_chunk;
CREATE TRIGGER trg_document_chunk_tsv
    BEFORE INSERT OR UPDATE ON document_chunk
    FOR EACH ROW EXECUTE FUNCTION update_document_chunk_tsv();

CREATE INDEX IF NOT EXISTS idx_document_chunk_tsv ON document_chunk USING GIN (tsv_content);
