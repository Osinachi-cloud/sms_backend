-- Fix quiz_questions question_type check constraint
-- PostgreSQL does not support ALTER CONSTRAINT, so we drop and recreate.

-- 1. Drop the old check constraint (ignore if already gone)
ALTER TABLE quiz_questions DROP CONSTRAINT IF EXISTS quiz_questions_question_type_check;

-- 2. Add a new check constraint that includes the new question types
ALTER TABLE quiz_questions
    ADD CONSTRAINT quiz_questions_question_type_check
    CHECK (question_type IN ('MCQ', 'CHECKBOX', 'SHORT_ANSWER', 'PARAGRAPH', 'TRUE_FALSE', 'FILL_BLANK', 'MATCHING'));
