-- =====================================================
-- V27: Ensure role_permissions unique constraint exists.
-- This guards against environments where the table was
-- created without UNIQUE(role_id, permission_key).
-- =====================================================

-- If duplicates exist, remove them first (keep the oldest row)
DELETE FROM role_permissions
WHERE id IN (
    SELECT id FROM (
        SELECT
            id,
            ROW_NUMBER() OVER (
                PARTITION BY role_id, permission_key
                ORDER BY created_at ASC
            ) AS rn
        FROM role_permissions
    ) t
    WHERE t.rn > 1
);

-- Add the unique constraint only if it doesn't already exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conrelid = 'role_permissions'::regclass
          AND conname = 'role_permissions_role_id_permission_key_unique'
    ) THEN
        ALTER TABLE role_permissions
        ADD CONSTRAINT role_permissions_role_id_permission_key_unique
        UNIQUE (role_id, permission_key);
    END IF;
END $$;
