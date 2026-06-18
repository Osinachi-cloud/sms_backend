-- =====================================================
-- Assign user.read and user.create to all existing
-- SUPER_ADMIN and ADMIN roles across every school.
-- This is idempotent and safe to re-run.
-- =====================================================

-- Grant user.read to SUPER_ADMIN and ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'user.read', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'user.read'
  );

-- Grant user.create to SUPER_ADMIN and ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'user.create', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
  AND NOT EXISTS (
      SELECT 1 FROM role_permissions rp
      WHERE rp.role_id = r.id AND rp.permission_key = 'user.create'
  );
