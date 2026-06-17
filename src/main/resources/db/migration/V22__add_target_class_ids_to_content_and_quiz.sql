ALTER TABLE course_contents ADD COLUMN IF NOT EXISTS target_class_ids JSONB DEFAULT '[]';
ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS target_class_ids JSONB DEFAULT '[]';
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS target_class_ids JSONB DEFAULT '[]';
