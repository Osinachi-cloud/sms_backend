-- =====================================================
-- Subject enhancements and temporary user permissions
-- =====================================================

-- Add created_by_type to subjects to track who created the subject
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS created_by_type VARCHAR(20) DEFAULT 'ADMIN';

-- Create temporary user permissions table for super admin privilege assignment
CREATE TABLE IF NOT EXISTS temporary_user_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    permission_key VARCHAR(100) NOT NULL REFERENCES permissions(key) ON DELETE CASCADE,
    granted_by UUID REFERENCES users(id),
    expires_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, school_id, permission_key)
);

CREATE INDEX IF NOT EXISTS idx_temp_permissions_user_school ON temporary_user_permissions(user_id, school_id);
CREATE INDEX IF NOT EXISTS idx_temp_permissions_expires ON temporary_user_permissions(expires_at);

-- Add new permissions for product creation and payment gateway switching
INSERT INTO permissions (id, key, category, description, created_at)
VALUES
    (uuid_generate_v4(), 'product.create', 'FINANCE', 'Create sellable products', NOW()),
    (uuid_generate_v4(), 'payment.gateway.switch', 'FINANCE', 'Switch active payment gateway', NOW()),
    (uuid_generate_v4(), 'cms.content.edit.any', 'CMS', 'Edit any content including teacher-created', NOW())
ON CONFLICT (key) DO NOTHING;

-- Assign product.create and payment.gateway.switch to SUPER_ADMIN and ADMIN roles
INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'product.create', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')
  AND r.is_active = true
ON CONFLICT (role_id, permission_key) DO NOTHING;

INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'payment.gateway.switch', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN', 'FINANCE_MANAGER')
  AND r.is_active = true
ON CONFLICT (role_id, permission_key) DO NOTHING;

INSERT INTO role_permissions (id, role_id, permission_key, created_at)
SELECT uuid_generate_v4(), r.id, 'cms.content.edit.any', NOW()
FROM roles r
WHERE r.name IN ('SUPER_ADMIN', 'ADMIN')
  AND r.is_active = true
ON CONFLICT (role_id, permission_key) DO NOTHING;
