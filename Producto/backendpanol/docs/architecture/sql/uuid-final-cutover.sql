-- UUID Final Cutover (manual runbook script)
-- Scope: remove legacy public numeric identifiers after full UUID-only validation.
-- WARNING:
-- 1) Run only in maintenance window.
-- 2) Run only after backend/frontend are UUID-only and no path/query/body uses numeric IDs.
-- 3) Take full backup/snapshot first.

BEGIN;

-- Safety checks (must return 0 rows)
-- SELECT * FROM migration_check_uuid_nulls;
-- SELECT * FROM migration_check_uuid_fk_orphans;

-- 1) Drop legacy compatibility claims/fields where applicable (example table names).
-- ALTER TABLE token_revocation DROP COLUMN IF EXISTS user_id;

-- 2) Drop legacy numeric FKs in child tables (example pattern; adjust to real schema).
-- ALTER TABLE loan_detail DROP CONSTRAINT IF EXISTS loan_detail_loan_id_fkey;
-- ALTER TABLE loan_detail DROP COLUMN IF EXISTS loan_id;
-- ALTER TABLE loan DROP CONSTRAINT IF EXISTS loan_user_id_fkey;
-- ALTER TABLE loan DROP COLUMN IF EXISTS user_id;
-- ALTER TABLE inventory_movement DROP CONSTRAINT IF EXISTS inventory_movement_implement_id_fkey;
-- ALTER TABLE inventory_movement DROP COLUMN IF EXISTS implement_id;

-- 3) Drop legacy numeric FK columns in catalog tables after UUID FK coverage is complete.
-- ALTER TABLE implement DROP CONSTRAINT IF EXISTS implement_category_id_fkey;
-- ALTER TABLE implement DROP COLUMN IF EXISTS category_id;
-- ALTER TABLE implement DROP CONSTRAINT IF EXISTS implement_location_id_fkey;
-- ALTER TABLE implement DROP COLUMN IF EXISTS location_id;
-- ALTER TABLE individual DROP CONSTRAINT IF EXISTS individual_current_location_id_fkey;
-- ALTER TABLE individual DROP COLUMN IF EXISTS current_location_id;

-- 4) Optional: move UUID column as PK in business tables (requires prepared FK topology).
-- ALTER TABLE "user" DROP CONSTRAINT IF EXISTS user_pkey;
-- ALTER TABLE "user" ADD CONSTRAINT user_pkey PRIMARY KEY (uuid);

-- 5) Remove legacy unique/indexes/sequences no longer used.
-- DROP SEQUENCE IF EXISTS user_id_seq;
-- DROP SEQUENCE IF EXISTS implement_id_seq;

COMMIT;

