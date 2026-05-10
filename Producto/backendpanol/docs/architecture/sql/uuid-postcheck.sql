-- UUID postcheck (safe, read-only)
-- Execute after final cutover.

-- 1) Legacy public endpoints should no longer be used by clients (manual check in app logs).
-- 2) Validate UUID PK/FK paths still consistent.

-- Null UUIDs must remain zero
SELECT 'user' AS table_name, COUNT(*) AS null_uuids FROM public."user" WHERE uuid IS NULL
UNION ALL
SELECT 'implement', COUNT(*) FROM public.implement WHERE uuid IS NULL
UNION ALL
SELECT 'category', COUNT(*) FROM public.category WHERE uuid IS NULL
UNION ALL
SELECT 'location', COUNT(*) FROM public.location WHERE uuid IS NULL
UNION ALL
SELECT 'individual', COUNT(*) FROM public.individual WHERE uuid IS NULL;

-- Optional: confirm legacy columns removed (if full cutover already executed)
-- SELECT column_name
-- FROM information_schema.columns
-- WHERE table_schema='public'
--   AND column_name ~ '(^id$|_id$)'
-- ORDER BY table_name, column_name;

