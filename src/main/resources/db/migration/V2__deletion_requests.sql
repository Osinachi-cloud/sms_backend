-- Deletion requests table for maker-checker workflow
-- This table is also defined in V1. Using IF NOT EXISTS and guarded blocks for consistency.

CREATE TABLE IF NOT EXISTS deletion_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL REFERENCES schools(id),
    requested_by UUID NOT NULL REFERENCES users(id),
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    -- SUPER_ADMIN review
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,
    review_notes TEXT,

    -- GENERAL_ADMIN forwarding
    forwarded_by UUID REFERENCES users(id),
    forwarded_at TIMESTAMP,
    forward_notes TEXT,

    -- APP_ADMIN final decision
    decided_by UUID REFERENCES users(id),
    decided_at TIMESTAMP,
    decision_notes TEXT,

    -- Timestamps
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Check for missing columns in deletion_requests (reconcile with current expected state)
ALTER TABLE deletion_requests ADD COLUMN IF NOT EXISTS review_notes TEXT;
ALTER TABLE deletion_requests ADD COLUMN IF NOT EXISTS forward_notes TEXT;
ALTER TABLE deletion_requests ADD COLUMN IF NOT EXISTS decided_by UUID;
ALTER TABLE deletion_requests ADD COLUMN IF NOT EXISTS decided_at TIMESTAMP;
ALTER TABLE deletion_requests ADD COLUMN IF NOT EXISTS decision_notes TEXT;

-- Status: PENDING -> REVIEWED -> FORWARDED -> APPROVED/REJECTED

CREATE INDEX IF NOT EXISTS idx_deletion_requests_school ON deletion_requests(school_id);
CREATE INDEX IF NOT EXISTS idx_deletion_requests_status ON deletion_requests(status);
CREATE INDEX IF NOT EXISTS idx_deletion_requests_requested_by ON deletion_requests(requested_by);

-- School backup table for soft-delete with data preservation
CREATE TABLE IF NOT EXISTS school_backups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL,
    school_data JSONB NOT NULL,
    students_data JSONB,
    teachers_data JSONB,
    classes_data JSONB,
    content_data JSONB,
    payments_data JSONB,
    deletion_request_id UUID REFERENCES deletion_requests(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_school_backups_school ON school_backups(school_id);
CREATE INDEX IF NOT EXISTS idx_school_backups_deletion_request ON school_backups(deletion_request_id);

-- Add trigger for updated_at
CREATE OR REPLACE FUNCTION update_deletion_request_timestamp()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DO $$
BEGIN
    CREATE TRIGGER deletion_request_timestamp
        BEFORE UPDATE ON deletion_requests
        FOR EACH ROW
        EXECUTE FUNCTION update_deletion_request_timestamp();
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
