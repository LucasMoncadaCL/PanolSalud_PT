-- Cleanup legacy dual-key triggers/functions after UUID-only cutover.

DROP TRIGGER IF EXISTS trg_sync_user_role_keys ON public."user";
DROP FUNCTION IF EXISTS public.fn_sync_user_role_keys();

-- UUID-only user trigger: keeps auth_uuid in sync when blank.
CREATE OR REPLACE FUNCTION public.fn_sync_user_auth_uuid_only()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.auth_uuid IS NULL OR NEW.auth_uuid = '' THEN
        NEW.auth_uuid := NEW.uuid::text;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sync_user_auth_uuid_only ON public."user";
CREATE TRIGGER trg_sync_user_auth_uuid_only
BEFORE INSERT OR UPDATE ON public."user"
FOR EACH ROW
EXECUTE FUNCTION public.fn_sync_user_auth_uuid_only();

DROP FUNCTION IF EXISTS public.fn_validate_uuid_migration();
