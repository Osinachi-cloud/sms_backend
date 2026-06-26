-- =====================================================
-- Grading Schemes, Quiz Enhancements, and Gradebook
-- =====================================================

-- 1. Grading Schemes and Components
CREATE TABLE IF NOT EXISTS grading_schemes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    is_default BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS grading_components (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    scheme_id UUID REFERENCES grading_schemes(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    weight INT NOT NULL CHECK (weight >= 0 AND weight <= 100),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 2. Quiz Selections and History
CREATE TABLE IF NOT EXISTS quiz_selection_history (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID REFERENCES teachers(id) ON DELETE CASCADE,
    subject_id UUID REFERENCES subjects(id) ON DELETE CASCADE,
    class_id UUID REFERENCES classes(id) ON DELETE CASCADE,
    snapshot JSONB NOT NULL,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS quiz_selection_pointers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    teacher_id UUID REFERENCES teachers(id) ON DELETE CASCADE,
    subject_id UUID REFERENCES subjects(id) ON DELETE CASCADE,
    class_id UUID REFERENCES classes(id) ON DELETE CASCADE,
    current_index INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(teacher_id, subject_id, class_id)
);

-- 3. Update Existing Tables
ALTER TABLE subjects ADD COLUMN IF NOT EXISTS grading_scheme_id UUID REFERENCES grading_schemes(id) ON DELETE SET NULL;

ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS is_selected_for_grade BOOLEAN DEFAULT FALSE;
ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS selected_at TIMESTAMP;
ALTER TABLE quizzes ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP;

-- 4. Grade Boundaries for Report Card
CREATE TABLE IF NOT EXISTS grade_boundaries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    grade_letter VARCHAR(5) NOT NULL,
    min_score DECIMAL(5,2) NOT NULL,
    max_score DECIMAL(5,2) NOT NULL,
    remarks TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

-- 5. Triggers for UPDATED_AT
DO $$
BEGIN
    CREATE TRIGGER update_grading_schemes_updated_at BEFORE UPDATE ON grading_schemes
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TRIGGER update_quiz_selection_pointers_updated_at BEFORE UPDATE ON quiz_selection_pointers
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TRIGGER update_grade_boundaries_updated_at BEFORE UPDATE ON grade_boundaries
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
