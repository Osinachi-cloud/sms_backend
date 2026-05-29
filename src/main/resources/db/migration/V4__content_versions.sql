-- Content versions for version history
-- Table creation is handled in V1, ensuring consistency
-- This migration ensures the table exists and has the correct columns if it was somehow skipped or altered

DO $$ 
BEGIN
    IF NOT EXISTS (SELECT FROM pg_tables WHERE schemaname = 'public' AND tablename  = 'content_versions') THEN
        CREATE TABLE content_versions (
            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
            content_id UUID NOT NULL REFERENCES content_items(id) ON DELETE CASCADE,
            version_number INT NOT NULL,
            title VARCHAR(255) NOT NULL,
            body TEXT,
            created_by UUID NOT NULL REFERENCES users(id),
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            change_summary VARCHAR(500)
        );
    END IF;
END $$;

-- Check for missing columns in content_versions (reconcile V1 vs V4 expected state)
ALTER TABLE content_versions ADD COLUMN IF NOT EXISTS version_number INT;
ALTER TABLE content_versions ADD COLUMN IF NOT EXISTS created_by UUID;

-- Ensure constraints for content_versions if they are missing
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.table_constraints WHERE table_name = 'content_versions' AND constraint_name = 'content_versions_created_by_fkey') THEN
        ALTER TABLE content_versions ADD CONSTRAINT content_versions_created_by_fkey FOREIGN KEY (created_by) REFERENCES users(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_content_versions_content ON content_versions(content_id);

-- Fix for ERROR: column "version_number" does not exist (ensure it exists before index)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='content_versions' AND column_name='version_number') THEN
        IF NOT EXISTS (SELECT 1 FROM pg_class c JOIN pg_index i ON c.oid = i.indexrelid JOIN pg_class t ON t.oid = i.indrelid WHERE t.relname = 'content_versions' AND c.relname = 'idx_content_versions_number') THEN
            CREATE INDEX idx_content_versions_number ON content_versions(content_id, version_number DESC);
        END IF;
    END IF;
END $$;

-- Add scheduled publishing fields to content_items if missing
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS scheduled_publish_at TIMESTAMP;
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS published_at TIMESTAMP;
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS version INT DEFAULT 1;
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS current_version INT DEFAULT 1;
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS is_featured BOOLEAN DEFAULT FALSE;
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS view_count INT DEFAULT 0;
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS tags TEXT[];

-- Create index for scheduled publishing
DROP INDEX IF EXISTS idx_content_scheduled;
CREATE INDEX idx_content_scheduled ON content_items(scheduled_publish_at) WHERE scheduled_publish_at IS NOT NULL AND status = 'SCHEDULED';

-- Create index for featured content
DROP INDEX IF EXISTS idx_content_featured;
CREATE INDEX idx_content_featured ON content_items(school_id, is_featured) WHERE is_featured = TRUE;
