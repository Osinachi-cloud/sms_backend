-- Migration: Add new quiz columns for Exams, Tests & Quizzes feature
-- Run this against your PostgreSQL database if restarting the backend
-- does not auto-create the columns (e.g., if ddl-auto is set to validate).

ALTER TABLE quizzes
    ADD COLUMN IF NOT EXISTS quiz_type VARCHAR(20) DEFAULT 'QUIZ',
    ADD COLUMN IF NOT EXISTS is_enabled BOOLEAN DEFAULT true,
    ADD COLUMN IF NOT EXISTS show_correct_answers BOOLEAN DEFAULT false,
    ADD COLUMN IF NOT EXISTS term_id UUID,
    ADD COLUMN IF NOT EXISTS session_id UUID;
