-- UUID-ONLY CUTOVER (safe/idempotent for Supabase current schema)
-- This migration avoids destructive PK replacement in-place because
-- existing primary keys and dependent constraints require a coordinated
-- table-by-table rewire with precomputed dependency order.
-- It enforces UUID completeness and prepares strict checks for final drop.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name = 'uuid'
    LOOP
        EXECUTE format('UPDATE public.%I SET uuid = gen_random_uuid() WHERE uuid IS NULL', rec.table_name);
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN uuid SET NOT NULL', rec.table_name);
        EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS %I ON public.%I(uuid)', 'ux_' || rec.table_name || '_uuid_final', rec.table_name);
    END LOOP;
END
$$;

-- Legacy auth/audit numeric references were already removed in V11.
-- Remaining numeric identifiers will be removed in a dependency-aware final migration.
