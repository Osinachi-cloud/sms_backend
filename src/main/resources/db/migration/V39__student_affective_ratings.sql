CREATE TABLE IF NOT EXISTS student_affective_ratings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL,
    student_id UUID NOT NULL,
    term_id UUID NOT NULL,
    trait VARCHAR(50) NOT NULL,
    rating INTEGER,
    remarks VARCHAR(255),
    rated_by UUID,
    created_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE DEFAULT NOW(),
    CONSTRAINT fk_affective_school FOREIGN KEY (school_id) REFERENCES schools(id) ON DELETE CASCADE,
    CONSTRAINT fk_affective_student FOREIGN KEY (student_id) REFERENCES students(id) ON DELETE CASCADE,
    CONSTRAINT fk_affective_term FOREIGN KEY (term_id) REFERENCES terms(id) ON DELETE CASCADE,
    CONSTRAINT fk_affective_rater FOREIGN KEY (rated_by) REFERENCES users(id) ON DELETE SET NULL,
    CONSTRAINT uq_student_term_trait UNIQUE (student_id, term_id, trait)
);

CREATE INDEX idx_affective_student ON student_affective_ratings(student_id);
CREATE INDEX idx_affective_term ON student_affective_ratings(term_id);
CREATE INDEX idx_affective_school ON student_affective_ratings(school_id);
