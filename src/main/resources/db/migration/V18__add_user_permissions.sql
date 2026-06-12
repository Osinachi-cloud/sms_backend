-- =====================================================
-- Add user management permissions to master list
-- This is idempotent and safe to re-run
-- =====================================================

INSERT INTO permissions (id, key, category, description, created_at)
VALUES
    (uuid_generate_v4(), 'user.read', 'USER', 'View school users', NOW()),
    (uuid_generate_v4(), 'user.create', 'USER', 'Create school users', NOW())
ON CONFLICT (key) DO NOTHING;
