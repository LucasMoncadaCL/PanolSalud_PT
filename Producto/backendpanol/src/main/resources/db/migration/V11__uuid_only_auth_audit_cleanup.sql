-- UUID-only final cleanup for auth/audit surfaces.
-- Safe for pre-production reset strategy.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Ensure UUID columns exist and are backfilled before dropping legacy numeric refs.
ALTER TABLE public.audit_log
    ADD COLUMN IF NOT EXISTS actor_user_uuid uuid,
    ADD COLUMN IF NOT EXISTS target_user_uuid uuid;

ALTER TABLE public.token_revocation
    ADD COLUMN IF NOT EXISTS user_uuid uuid;

UPDATE public.audit_log a
SET actor_user_uuid = u.uuid
FROM public."user" u
WHERE a.actor_user_uuid IS NULL
  AND a.actor_user_id IS NOT NULL
  AND a.actor_user_id = u.id;

UPDATE public.audit_log a
SET target_user_uuid = u.uuid
FROM public."user" u
WHERE a.target_user_uuid IS NULL
  AND a.target_user_id IS NOT NULL
  AND a.target_user_id = u.id;

UPDATE public.token_revocation t
SET user_uuid = u.uuid
FROM public."user" u
WHERE t.user_uuid IS NULL
  AND t.user_id IS NOT NULL
  AND t.user_id = u.id;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_audit_actor_user_uuid') THEN
        ALTER TABLE public.audit_log
            ADD CONSTRAINT fk_audit_actor_user_uuid
            FOREIGN KEY (actor_user_uuid) REFERENCES public."user"(uuid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_audit_target_user_uuid') THEN
        ALTER TABLE public.audit_log
            ADD CONSTRAINT fk_audit_target_user_uuid
            FOREIGN KEY (target_user_uuid) REFERENCES public."user"(uuid);
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_token_revocation_user_uuid') THEN
        ALTER TABLE public.token_revocation
            ADD CONSTRAINT fk_token_revocation_user_uuid
            FOREIGN KEY (user_uuid) REFERENCES public."user"(uuid);
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_audit_actor_uuid ON public.audit_log(actor_user_uuid);
CREATE INDEX IF NOT EXISTS idx_audit_target_uuid ON public.audit_log(target_user_uuid);
CREATE INDEX IF NOT EXISTS idx_token_revocation_user_uuid ON public.token_revocation(user_uuid);

-- Drop legacy numeric references.
ALTER TABLE public.audit_log
    DROP COLUMN IF EXISTS actor_user_id,
    DROP COLUMN IF EXISTS target_user_id;

ALTER TABLE public.token_revocation
    DROP COLUMN IF EXISTS user_id;

-- Post-cutover checks
CREATE OR REPLACE VIEW public.migration_check_uuid_auth_audit AS
SELECT 'audit_log.actor_user_uuid.null'::text AS metric, COUNT(*)::bigint AS pending
FROM public.audit_log
WHERE actor_user_uuid IS NULL
UNION ALL
SELECT 'audit_log.target_user_uuid.null'::text AS metric, COUNT(*)::bigint AS pending
FROM public.audit_log
WHERE target_user_uuid IS NULL
UNION ALL
SELECT 'token_revocation.user_uuid.null'::text AS metric, COUNT(*)::bigint AS pending
FROM public.token_revocation
WHERE user_uuid IS NULL;
