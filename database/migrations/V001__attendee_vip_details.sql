-- ============================================================
-- V001: Add VIP attendee details
--
-- Purpose:
--   Upgrade an existing EMS database with VIP attendee fields.
--
-- Safety:
--   - Existing attendee records are preserved.
--   - Existing VIP records must have a valid VIP level.
--   - Invalid records stop the migration with a clear error.
--   - Safe to execute more than once.
-- ============================================================

BEGIN;

-- 1. Add VIP-specific fields.
ALTER TABLE public.attendees
    ADD COLUMN IF NOT EXISTS vip_level VARCHAR(50);

ALTER TABLE public.attendees
    ADD COLUMN IF NOT EXISTS special_requirements TEXT;

-- 2. Validate existing records before adding the constraint.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM public.attendees
        WHERE
            (
                is_vip = TRUE
                AND (
                    vip_level IS NULL
                    OR LENGTH(BTRIM(vip_level)) = 0
                )
            )
            OR
            (
                is_vip = FALSE
                AND (
                    vip_level IS NOT NULL
                    OR special_requirements IS NOT NULL
                )
            )
    ) THEN
        RAISE EXCEPTION
            'VIP attendee migration stopped: existing attendee records do not satisfy the VIP details rules.'
        USING HINT =
            'Review existing attendees. Every VIP attendee needs a valid vip_level. Non-VIP attendees must have NULL vip_level and special_requirements. Correct the records deliberately, then rerun this migration.';
    END IF;

    -- 3. Add the constraint only if it does not already exist.
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'chk_attendees_vip_details'
          AND conrelid = 'public.attendees'::regclass
    ) THEN
        ALTER TABLE public.attendees
            ADD CONSTRAINT chk_attendees_vip_details
            CHECK (
                (
                    is_vip = TRUE
                    AND vip_level IS NOT NULL
                    AND LENGTH(BTRIM(vip_level)) > 0
                )
                OR
                (
                    is_vip = FALSE
                    AND vip_level IS NULL
                    AND special_requirements IS NULL
                )
            );
    END IF;
END
$$;

COMMIT;
