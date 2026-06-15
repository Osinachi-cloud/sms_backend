-- =====================================================
-- Add payment gateway permissions to master list
-- and assign them to all existing ADMIN roles.
-- This is idempotent and safe to re-run.
-- =====================================================

-- Ensure payment gateway permissions exist
INSERT INTO permissions (id, key, category, description, created_at)
VALUES
    (uuid_generate_v4(), 'payment.gateway.manage', 'PAYMENT', 'Manage payment gateway configuration', NOW()),
    (uuid_generate_v4(), 'payment.gateway.switch', 'PAYMENT', 'Switch active payment gateway', NOW())
ON CONFLICT (key) DO NOTHING;

-- Grant payment.gateway.manage to ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'payment.gateway.manage', NOW()
FROM roles r
WHERE r.name = 'ADMIN'
  AND r.is_active = true
ON CONFLICT (role_id, permission_key) DO NOTHING;

-- Grant payment.gateway.switch to ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'payment.gateway.switch', NOW()
FROM roles r
WHERE r.name = 'ADMIN'
  AND r.is_active = true
ON CONFLICT (role_id, permission_key) DO NOTHING;
