-- Add photo_url column to students table for passport photograph on ID cards
ALTER TABLE IF EXISTS students
ADD COLUMN IF NOT EXISTS photo_url TEXT;

CREATE INDEX IF NOT EXISTS idx_students_photo ON students(photo_url) WHERE photo_url IS NOT NULL;
