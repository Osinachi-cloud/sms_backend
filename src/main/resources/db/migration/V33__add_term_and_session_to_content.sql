-- =====================================================
-- Add term_id and session_id columns to content_items
-- and course_contents for academic term/session tracking.
-- =====================================================

ALTER TABLE content_items
    ADD COLUMN IF NOT EXISTS term_id UUID,
    ADD COLUMN IF NOT EXISTS session_id UUID;

ALTER TABLE course_contents
    ADD COLUMN IF NOT EXISTS term_id UUID,
    ADD COLUMN IF NOT EXISTS session_id UUID;
