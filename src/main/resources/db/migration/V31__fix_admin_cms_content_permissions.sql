-- =====================================================
-- Fix missing CMS content permissions for ADMIN/SUPER_ADMIN roles.
-- The original seeder forgot to grant cms.content.create,
-- cms.content.edit, and cms.content.edit.any to school admins.
-- This is idempotent and safe to re-run.
-- =====================================================

-- Ensure the three missing permissions exist in the master list
INSERT INTO permissions (id, key, category, description, created_at)
VALUES
    (uuid_generate_v4(), 'cms.content.create', 'CMS', 'Create CMS content', NOW()),
    (uuid_generate_v4(), 'cms.content.edit', 'CMS', 'Edit own content', NOW()),
    (uuid_generate_v4(), 'cms.content.edit.any', 'CMS', 'Edit any content', NOW())
ON CONFLICT (key) DO NOTHING;

-- Grant cms.content.create to SUPER_ADMIN and ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'cms.content.create', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'cms.content.create'
  );

-- Grant cms.content.edit to SUPER_ADMIN and ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'cms.content.edit', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'cms.content.edit'
  );

-- Grant cms.content.edit.any to SUPER_ADMIN and ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'cms.content.edit.any', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'cms.content.edit.any'
  );
