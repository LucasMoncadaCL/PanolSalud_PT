CREATE EXTENSION IF NOT EXISTS pgcrypto;

DO $$
DECLARE
    rec RECORD;
BEGIN
    FOR rec IN
        SELECT c.table_name
        FROM information_schema.columns c
        WHERE c.table_schema = 'public'
          AND c.column_name = 'id'
          AND c.table_name <> 'flyway_schema_history'
    LOOP
        EXECUTE format('ALTER TABLE public.%I ADD COLUMN IF NOT EXISTS uuid uuid', rec.table_name);
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN uuid SET DEFAULT gen_random_uuid()', rec.table_name);
        EXECUTE format('UPDATE public.%I SET uuid = gen_random_uuid() WHERE uuid IS NULL', rec.table_name);
        EXECUTE format('ALTER TABLE public.%I ALTER COLUMN uuid SET NOT NULL', rec.table_name);
        EXECUTE format('CREATE UNIQUE INDEX IF NOT EXISTS %I ON public.%I (uuid)', 'ux_' || rec.table_name || '_uuid', rec.table_name);
    END LOOP;
END
$$;

ALTER TABLE public."user"
    ADD COLUMN IF NOT EXISTS auth_uuid varchar(255);

UPDATE public."user"
SET auth_uuid = uuid::text
WHERE auth_uuid IS NULL OR auth_uuid = '';

CREATE UNIQUE INDEX IF NOT EXISTS ux_user_auth_uuid ON public."user"(auth_uuid);
