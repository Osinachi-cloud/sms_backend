-- Grant missing CMS permissions to existing TEACHER roles
-- Fixes: teachers couldn't see "New Folder" / "New Content" buttons
-- because older databases never received these permissions.

-- 1) Ensure a unique constraint exists on permissions.key (needed for idempotent inserts)
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'uk_permissions_key'
    ) AND NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'uk_permissions_key'
    ) THEN
        CREATE UNIQUE INDEX uk_permissions_key ON permissions(key);
    END IF;
END $$;

-- 2) Ensure the permission rows exist in the master permissions table
INSERT INTO permissions (id, key, category, description, created_at)
VALUES
    (gen_random_uuid(), 'cms.folder.create', 'CMS', 'Create CMS folders', NOW()),
    (gen_random_uuid(), 'cms.content.delete', 'CMS', 'Delete content', NOW())
ON CONFLICT (key) DO NOTHING;

-- 3) Grant cms.folder.create to every existing TEACHER role (skip if already granted)
INSERT INTO role_permissions (role_id, permission_key)
SELECT r.id, 'cms.folder.create'
FROM roles r
WHERE r.name = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'cms.folder.create'
  );

-- 4) Grant cms.content.delete to every existing TEACHER role (skip if already granted)
INSERT INTO role_permissions (role_id, permission_key)
SELECT r.id, 'cms.content.delete'
FROM roles r
WHERE r.name = 'TEACHER'
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'cms.content.delete'
  );
