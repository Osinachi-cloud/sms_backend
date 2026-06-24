-- =====================================================
-- Fix missing CMS content permissions for TEACHER roles.
-- V30 only added cms.folder.create and cms.content.delete
-- to teachers but forgot cms.content.create, cms.content.edit,
-- and cms.content.edit.any. This migration backfills them
-- for every existing TEACHER role.
-- Idempotent and safe to re-run.
-- =====================================================

-- 1) Ensure the missing permission rows exist
INSERT INTO permissions (id, key, category, description, created_at)
VALUES
    (gen_random_uuid(), 'cms.content.create', 'CMS', 'Create CMS content', NOW()),
    (gen_random_uuid(), 'cms.content.edit',   'CMS', 'Edit own content',   NOW()),
    (gen_random_uuid(), 'cms.content.edit.any','CMS', 'Edit any content',  NOW())
ON CONFLICT (key) DO NOTHING;

-- 2) Grant cms.content.create to every TEACHER role
INSERT INTO role_permissions (id, role_id, permission_key)
SELECT gen_random_uuid(), r.id, 'cms.content.create'
FROM roles r
WHERE r.name = 'TEACHER'
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'cms.content.create'
  );

-- 3) Grant cms.content.edit to every TEACHER role
INSERT INTO role_permissions (id, role_id, permission_key)
SELECT gen_random_uuid(), r.id, 'cms.content.edit'
FROM roles r
WHERE r.name = 'TEACHER'
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'cms.content.edit'
  );

-- 4) Grant cms.content.edit.any to every TEACHER role
INSERT INTO role_permissions (id, role_id, permission_key)
SELECT gen_random_uuid(), r.id, 'cms.content.edit.any'
FROM roles r
WHERE r.name = 'TEACHER'
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'cms.content.edit.any'
  );
