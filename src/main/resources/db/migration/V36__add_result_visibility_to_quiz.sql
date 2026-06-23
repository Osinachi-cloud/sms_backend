-- Migration: Add result visibility timing controls for quizzes
-- Allows teachers to control when students see their results

ALTER TABLE quizzes
    ADD COLUMN IF NOT EXISTS result_visibility_type VARCHAR(30) DEFAULT 'NEVER',
    ADD COLUMN IF NOT EXISTS results_released BOOLEAN DEFAULT false;
