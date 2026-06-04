-- =====================================================
-- Revert: Convert DOUBLE PRECISION columns back to DECIMAL
-- so Hibernate BigDecimal fields validate correctly.
-- V19 incorrectly assumed Double mappings.
-- =====================================================

-- V10: CBT Quiz System
ALTER TABLE quizzes ALTER COLUMN total_marks TYPE DECIMAL(5,2);
ALTER TABLE quizzes ALTER COLUMN pass_mark TYPE DECIMAL(5,2);
ALTER TABLE quiz_questions ALTER COLUMN marks TYPE DECIMAL(5,2);
ALTER TABLE quiz_submissions ALTER COLUMN score TYPE DECIMAL(5,2);
ALTER TABLE quiz_submissions ALTER COLUMN total_marks TYPE DECIMAL(5,2);
ALTER TABLE quiz_submissions ALTER COLUMN percentage TYPE DECIMAL(5,2);
ALTER TABLE quiz_answers ALTER COLUMN marks_obtained TYPE DECIMAL(5,2);

-- V12: Digital Library
ALTER TABLE book_borrowals ALTER COLUMN fine_amount TYPE DECIMAL(10,2);

-- V14: Admission Portal
ALTER TABLE admission_applications ALTER COLUMN exam_score TYPE DECIMAL(5,2);
ALTER TABLE admission_applications ALTER COLUMN interview_score TYPE DECIMAL(5,2);

-- V15: Report Cards
ALTER TABLE report_cards ALTER COLUMN total_score TYPE DECIMAL(5,2);
ALTER TABLE report_cards ALTER COLUMN average_score TYPE DECIMAL(5,2);
ALTER TABLE report_card_entries ALTER COLUMN test_score TYPE DECIMAL(5,2);
ALTER TABLE report_card_entries ALTER COLUMN exam_score TYPE DECIMAL(5,2);
ALTER TABLE report_card_entries ALTER COLUMN total_score TYPE DECIMAL(5,2);
