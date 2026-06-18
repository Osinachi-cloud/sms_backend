-- =====================================================
-- V26: Ensure timetable permissions exist and are granted
-- to all existing system roles (ADMIN, TEACHER, STUDENT).
-- This is idempotent and safe to re-run.
-- =====================================================

-- 1. Ensure all timetable permission rows exist in the master permissions list
INSERT INTO permissions (id, key, category, description, created_at)
VALUES
    (uuid_generate_v4(), 'timetable.read',  'TIMETABLE', 'View timetable',         NOW()),
    (uuid_generate_v4(), 'timetable.create','TIMETABLE', 'Create timetable entries',NOW()),
    (uuid_generate_v4(), 'timetable.update','TIMETABLE', 'Update timetable entries',NOW()),
    (uuid_generate_v4(), 'timetable.delete','TIMETABLE', 'Delete timetable entries',NOW())
ON CONFLICT (key) DO NOTHING;

-- 2. Grant ALL timetable permissions to ADMIN / SUPER_ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, p.key, NOW()
FROM roles r
CROSS JOIN (VALUES
    ('timetable.read'),
    ('timetable.create'),
    ('timetable.update'),
    ('timetable.delete')
) AS p(key)
WHERE r.name IN ('ADMIN', 'SUPER_ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = p.key
  );

-- 3. Grant ALL timetable permissions to TEACHER roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, p.key, NOW()
FROM roles r
CROSS JOIN (VALUES
    ('timetable.read'),
    ('timetable.create'),
    ('timetable.update'),
    ('timetable.delete')
) AS p(key)
WHERE r.name = 'TEACHER'
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = p.key
  );

-- 4. Grant timetable.read to STUDENT roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'timetable.read', NOW()
FROM roles r
WHERE r.name = 'STUDENT'
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'timetable.read'
  );
