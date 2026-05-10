-- UUID precheck (safe, read-only)
-- Execute before final cutover.

-- 1) Null UUIDs in core business tables
SELECT 'user' AS table_name, COUNT(*) AS null_uuids FROM public."user" WHERE uuid IS NULL
UNION ALL
SELECT 'implement', COUNT(*) FROM public.implement WHERE uuid IS NULL
UNION ALL
SELECT 'category', COUNT(*) FROM public.category WHERE uuid IS NULL
UNION ALL
SELECT 'location', COUNT(*) FROM public.location WHERE uuid IS NULL
UNION ALL
SELECT 'individual', COUNT(*) FROM public.individual WHERE uuid IS NULL;

-- 2) Orphans on UUID FKs (adjust column names if schema differs)
-- Expected result: 0 rows
SELECT loan.id AS orphan_loan_id
FROM public.loan
LEFT JOIN public."user" u ON u.uuid = loan.user_uuid
WHERE loan.user_uuid IS NOT NULL AND u.uuid IS NULL;

SELECT ld.id AS orphan_loan_detail_id
FROM public.loan_detail ld
LEFT JOIN public.loan l ON l.uuid = ld.loan_uuid
WHERE ld.loan_uuid IS NOT NULL AND l.uuid IS NULL;

-- 3) UUID uniqueness checks
-- Expected result: 0 rows
SELECT uuid, COUNT(*) FROM public."user" GROUP BY uuid HAVING COUNT(*) > 1;
SELECT uuid, COUNT(*) FROM public.implement GROUP BY uuid HAVING COUNT(*) > 1;
SELECT uuid, COUNT(*) FROM public.category GROUP BY uuid HAVING COUNT(*) > 1;
SELECT uuid, COUNT(*) FROM public.location GROUP BY uuid HAVING COUNT(*) > 1;
SELECT uuid, COUNT(*) FROM public.individual GROUP BY uuid HAVING COUNT(*) > 1;

