-- Post-cutover UUID-only integrity checks.

CREATE OR REPLACE VIEW public.migration_check_uuid_only_columns AS
SELECT table_name, column_name
FROM information_schema.columns
WHERE table_schema = 'public'
  AND (
      column_name = 'id'
      OR column_name LIKE '%\_id' ESCAPE '\'
  )
  AND table_name NOT IN ('flyway_schema_history', 'audit_log', 'token_revocation');

CREATE OR REPLACE VIEW public.migration_check_uuid_only_nulls AS
SELECT 'user.uuid.null'::text AS metric, COUNT(*)::bigint AS pending FROM public."user" WHERE uuid IS NULL
UNION ALL
SELECT 'role.uuid.null'::text AS metric, COUNT(*)::bigint AS pending FROM public.role WHERE uuid IS NULL
UNION ALL
SELECT 'category.uuid.null'::text AS metric, COUNT(*)::bigint AS pending FROM public.category WHERE uuid IS NULL
UNION ALL
SELECT 'location.uuid.null'::text AS metric, COUNT(*)::bigint AS pending FROM public.location WHERE uuid IS NULL
UNION ALL
SELECT 'implement.uuid.null'::text AS metric, COUNT(*)::bigint AS pending FROM public.implement WHERE uuid IS NULL
UNION ALL
SELECT 'individual.uuid.null'::text AS metric, COUNT(*)::bigint AS pending FROM public.individual WHERE uuid IS NULL;

CREATE OR REPLACE VIEW public.migration_check_uuid_only_fk_orphans AS
SELECT 'user.role_uuid.orphan'::text AS metric, COUNT(*)::bigint AS pending
FROM public."user" u
LEFT JOIN public.role r ON u.role_uuid = r.uuid
WHERE u.role_uuid IS NOT NULL AND r.uuid IS NULL
UNION ALL
SELECT 'implement.category_uuid.orphan'::text AS metric, COUNT(*)::bigint AS pending
FROM public.implement i
LEFT JOIN public.category c ON i.category_uuid = c.uuid
WHERE i.category_uuid IS NOT NULL AND c.uuid IS NULL
UNION ALL
SELECT 'implement.location_uuid.orphan'::text AS metric, COUNT(*)::bigint AS pending
FROM public.implement i
LEFT JOIN public.location l ON i.location_uuid = l.uuid
WHERE i.location_uuid IS NOT NULL AND l.uuid IS NULL;
