CREATE OR REPLACE FUNCTION update_article_tsv()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tsv_en := to_tsvector('english', COALESCE(NEW.title_en, '') || ' ' || COALESCE(NEW.content_en, ''));
    NEW.tsv_fr := to_tsvector('french', COALESCE(NEW.title_fr, '') || ' ' || COALESCE(NEW.content_fr, ''));
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trigger_article_tsv ON article;
CREATE TRIGGER trigger_article_tsv
BEFORE INSERT OR UPDATE OF title_en, content_en, title_fr, content_fr
ON article
FOR EACH ROW
EXECUTE FUNCTION update_article_tsv();
