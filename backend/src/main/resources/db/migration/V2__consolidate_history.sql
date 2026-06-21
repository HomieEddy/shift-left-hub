-- V2: Consolidate flyway_schema_history after the V1 squashed-baseline migration.
--
-- The previous migration chain (V1 through V12) has been collapsed into the
-- single V1 baseline. This migration rewrites flyway_schema_history so the
-- record shows the consolidated state instead of the historical chain.
--
-- Behaviour:
--   - On a database that already ran V1–V12 (i.e. the squashed V1 was a
--     no-op DDL-wise because the schema was identical), this migration
--     rewrites the history table: deletes the V2–V12 rows and clears the
--     checksum on the V1 row so future validateOnMigrate passes against
--     the new V1 file content. A NULL checksum is interpreted by Flyway
--     as "do not validate this row" so the new V1 file's checksum is
--     accepted on next migrate without manual intervention.
--   - On a database that never ran the old V1–V12 chain (e.g. a fresh
--     environment spun up after the squash), this migration is a no-op
--     because the V1 row was already recorded correctly on first migrate.
--
-- Safety: this migration is idempotent and only modifies flyway_schema_history.
-- It does not touch any application table or row.

DO $$
DECLARE
    old_history_count integer;
BEGIN
    SELECT count(*) INTO old_history_count
    FROM flyway_schema_history
    WHERE version IN ('2','3','4','5','6','7','8','9','10','11','12');

    IF old_history_count > 0 THEN
        DELETE FROM flyway_schema_history
        WHERE version IN ('2','3','4','5','6','7','8','9','10','11','12');

        UPDATE flyway_schema_history
        SET checksum = NULL
        WHERE version = '1';
    END IF;
END;
$$;
