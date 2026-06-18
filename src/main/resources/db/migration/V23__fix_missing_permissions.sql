-- =====================================================
-- Fix missing permissions and assign to existing roles
-- This is idempotent and safe to re-run
-- =====================================================

-- Insert missing permissions
INSERT INTO permissions (id, key, category, description, created_at)
VALUES
    (uuid_generate_v4(), 'cms.content.delete', 'CMS', 'Delete content', NOW()),
    (uuid_generate_v4(), 'user.delete', 'USER', 'Delete school users', NOW()),
    (uuid_generate_v4(), 'timetable.read', 'TIMETABLE', 'View timetable', NOW()),
    (uuid_generate_v4(), 'timetable.create', 'TIMETABLE', 'Create timetable entries', NOW()),
    (uuid_generate_v4(), 'timetable.update', 'TIMETABLE', 'Update timetable entries', NOW()),
    (uuid_generate_v4(), 'timetable.delete', 'TIMETABLE', 'Delete timetable entries', NOW())
ON CONFLICT (key) DO NOTHING;

-- Grant cms.content.delete to SUPER_ADMIN and ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'cms.content.delete', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'cms.content.delete'
  );

-- Grant user.delete to SUPER_ADMIN and ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'user.delete', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'user.delete'
  );

-- Grant timetable permissions to SUPER_ADMIN and ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'timetable.read', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'timetable.read'
  );

INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'timetable.create', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'timetable.create'
  );

INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'timetable.update', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'timetable.update'
  );

INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'timetable.delete', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'timetable.delete'
  );

-- Grant timetable.read and timetable.create to TEACHER roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'timetable.read', NOW()
FROM roles r
WHERE r.name = 'TEACHER'
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'timetable.read'
  );

INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'timetable.create', NOW()
FROM roles r
WHERE r.name = 'TEACHER'
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'timetable.create'
  );

-- Grant timetable.read to STUDENT roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'timetable.read', NOW()
FROM roles r
WHERE r.name = 'STUDENT'
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'timetable.read'
  );
