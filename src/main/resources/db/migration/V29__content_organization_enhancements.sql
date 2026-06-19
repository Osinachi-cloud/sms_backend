-- Content Organization Enhancements
-- Adds subject_id to content_items for easier querying by subject
-- Adds indices for folder-based content lookups

-- Add subject_id to content_items for direct subject association
ALTER TABLE content_items ADD COLUMN IF NOT EXISTS subject_id UUID;

-- Add index for content by folder
CREATE INDEX IF NOT EXISTS idx_content_items_folder_id ON content_items(folder_id);

-- Add index for content by subject
CREATE INDEX IF NOT EXISTS idx_content_items_subject_id ON content_items(subject_id);

-- Add index for content folder subject lookups
CREATE INDEX IF NOT EXISTS idx_content_folders_subject_id ON content_folders(subject_id);

-- Add index for content folder class lookups
CREATE INDEX IF NOT EXISTS idx_content_folders_class_id ON content_folders(class_id);

-- Ensure content_items has proper status values
-- (status column already exists from V1, no change needed)
