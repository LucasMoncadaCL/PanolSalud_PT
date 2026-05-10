-- FINAL DESTRUCTIVE UUID CUTOVER
-- Drops numeric identity columns only when UUID counterpart exists.

DO $$
DECLARE
    v RECORD;
BEGIN
    FOR v IN
        SELECT schemaname, viewname
        FROM pg_catalog.pg_views
        WHERE schemaname = 'public'
          AND viewname <> 'flyway_schema_history'
    LOOP
        EXECUTE format('DROP VIEW IF EXISTS %I.%I', v.schemaname, v.viewname);
    END LOOP;
END $$;

DO $$
DECLARE
    rec RECORD;
BEGIN
    -- Drop FK constraints that reference legacy *_id columns with *_uuid counterpart.
    FOR rec IN
        SELECT tc.constraint_name, tc.table_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_schema = kcu.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.constraint_type = 'FOREIGN KEY'
          AND kcu.column_name LIKE '%\_id' ESCAPE E'\\'
          AND EXISTS (
              SELECT 1
              FROM information_schema.columns c
              WHERE c.table_schema = kcu.table_schema
                AND c.table_name = kcu.table_name
                AND c.column_name = regexp_replace(kcu.column_name, '_id$', '_uuid')
          )
    LOOP
        EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT IF EXISTS %I', rec.table_name, rec.constraint_name);
    END LOOP;

    -- Drop PK constraints on id where uuid exists.
    FOR rec IN
        SELECT tc.constraint_name, tc.table_name
        FROM information_schema.table_constraints tc
        JOIN information_schema.key_column_usage kcu
          ON tc.constraint_name = kcu.constraint_name
         AND tc.table_schema = kcu.table_schema
        WHERE tc.table_schema = 'public'
          AND tc.constraint_type = 'PRIMARY KEY'
          AND kcu.column_name = 'id'
          AND EXISTS (
              SELECT 1 FROM information_schema.columns c
              WHERE c.table_schema = tc.table_schema
                AND c.table_name = tc.table_name
                AND c.column_name = 'uuid'
          )
    LOOP
        EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT IF EXISTS %I', rec.table_name, rec.constraint_name);
    END LOOP;

    -- Promote uuid as PK when absent.
    FOR rec IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name = 'uuid'
          AND NOT EXISTS (
              SELECT 1
              FROM information_schema.table_constraints tc
              JOIN information_schema.key_column_usage kcu
                ON tc.constraint_name = kcu.constraint_name
               AND tc.table_schema = kcu.table_schema
              WHERE tc.table_schema = c.table_schema
                AND tc.table_name = c.table_name
                AND tc.constraint_type = 'PRIMARY KEY'
                AND kcu.column_name = 'uuid'
          )
    LOOP
        EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I PRIMARY KEY (uuid)', rec.table_name, rec.table_name || '_pkey_uuid');
    END LOOP;

    -- Drop legacy *_id columns only if *_uuid exists.
    FOR rec IN
        SELECT c.table_name, c.column_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name LIKE '%\_id' ESCAPE E'\\'
          AND EXISTS (
              SELECT 1
              FROM information_schema.columns c2
              WHERE c2.table_schema = c.table_schema
                AND c2.table_name = c.table_name
                AND c2.column_name = regexp_replace(c.column_name, '_id$', '_uuid')
          )
    LOOP
        EXECUTE format('ALTER TABLE public.%I DROP COLUMN IF EXISTS %I', rec.table_name, rec.column_name);
    END LOOP;

    -- Drop plain id where uuid exists.
    FOR rec IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name = 'id'
          AND EXISTS (
              SELECT 1 FROM information_schema.columns c2
              WHERE c2.table_schema = c.table_schema
                AND c2.table_name = c.table_name
                AND c2.column_name = 'uuid'
          )
    LOOP
        EXECUTE format('ALTER TABLE public.%I DROP COLUMN IF EXISTS id', rec.table_name);
    END LOOP;
END $$;

-- Recreate stock summary view on UUID joins.
DROP VIEW IF EXISTS public.v_stock_summary;
CREATE VIEW public.v_stock_summary AS
SELECT
    i.uuid AS implement_uuid,
    i.name AS implement_name,
    i.item_type,
    COALESCE(s.total_stock, 0) AS total_stock,
    COALESCE(s.min_stock, 0) AS min_stock,
    COALESCE(s.available, 0) AS available,
    COALESCE(s.reserved, 0) AS reserved,
    COALESCE(s.loaned, 0) AS loaned,
    COALESCE(s.damaged, 0) AS damaged,
    CASE
        WHEN COALESCE(s.total_stock, 0) = 0 THEN 'SIN_STOCK'
        WHEN COALESCE(s.available, 0) <= COALESCE(s.min_stock, 0) THEN 'BAJO_MINIMO'
        ELSE 'OK'
    END AS stock_status,
    c.uuid AS category_uuid,
    c.name AS category_name,
    l.uuid AS location_uuid,
    l.name AS location_name,
    CASE
        WHEN COALESCE(s.loaned, 0) > 0 THEN 'Prestado'
        ELSE l.name
    END AS display_location
FROM public.implement i
LEFT JOIN public.stock s ON s.implement_uuid = i.uuid
LEFT JOIN public.category c ON c.uuid = i.category_uuid
LEFT JOIN public.location l ON l.uuid = i.location_uuid;
