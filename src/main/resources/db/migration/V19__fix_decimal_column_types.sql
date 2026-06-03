-- =====================================================
-- Fix: Convert DECIMAL columns to DOUBLE PRECISION
-- so Hibernate Double fields validate correctly
-- =====================================================

-- V10: CBT Quiz System
ALTER TABLE quizzes ALTER COLUMN total_marks TYPE DOUBLE PRECISION;
ALTER TABLE quizzes ALTER COLUMN pass_mark TYPE DOUBLE PRECISION;
ALTER TABLE quiz_questions ALTER COLUMN marks TYPE DOUBLE PRECISION;
ALTER TABLE quiz_submissions ALTER COLUMN score TYPE DOUBLE PRECISION;
ALTER TABLE quiz_submissions ALTER COLUMN total_marks TYPE DOUBLE PRECISION;
ALTER TABLE quiz_submissions ALTER COLUMN percentage TYPE DOUBLE PRECISION;
ALTER TABLE quiz_answers ALTER COLUMN marks_obtained TYPE DOUBLE PRECISION;

-- V12: Digital Library
ALTER TABLE book_borrowals ALTER COLUMN fine_amount TYPE DOUBLE PRECISION;

-- V14: Admission Portal
ALTER TABLE admission_applications ALTER COLUMN exam_score TYPE DOUBLE PRECISION;
ALTER TABLE admission_applications ALTER COLUMN interview_score TYPE DOUBLE PRECISION;

-- V15: Report Cards
ALTER TABLE report_cards ALTER COLUMN total_score TYPE DOUBLE PRECISION;
ALTER TABLE report_cards ALTER COLUMN average_score TYPE DOUBLE PRECISION;
ALTER TABLE report_card_entries ALTER COLUMN test_score TYPE DOUBLE PRECISION;
ALTER TABLE report_card_entries ALTER COLUMN exam_score TYPE DOUBLE PRECISION;
ALTER TABLE report_card_entries ALTER COLUMN total_score TYPE DOUBLE PRECISION;
