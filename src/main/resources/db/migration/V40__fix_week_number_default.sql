ALTER TABLE IF EXISTS student_affective_ratings
ADD COLUMN IF NOT EXISTS week_number INTEGER NOT NULL DEFAULT 0;

-- Update any existing rows that might still have NULL (safe-guard)
UPDATE student_affective_ratings SET week_number = 0 WHERE week_number IS NULL;
