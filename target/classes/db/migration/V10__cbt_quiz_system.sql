-- =====================================================
-- CBT / Online Quiz System
-- =====================================================

CREATE TABLE quizzes (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    subject_id UUID REFERENCES subjects(id),
    class_id UUID REFERENCES classes(id),
    created_by UUID REFERENCES users(id),
    duration_minutes INT DEFAULT 30,
    total_marks DECIMAL(5,2) DEFAULT 100,
    pass_mark DECIMAL(5,2) DEFAULT 40,
    shuffle_questions BOOLEAN DEFAULT FALSE,
    show_results_immediately BOOLEAN DEFAULT TRUE,
    max_attempts INT DEFAULT 1,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'CLOSED', 'ARCHIVED')),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE quiz_questions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quiz_id UUID REFERENCES quizzes(id) ON DELETE CASCADE,
    question_text TEXT NOT NULL,
    question_type VARCHAR(20) DEFAULT 'MCQ' CHECK (question_type IN ('MCQ', 'TRUE_FALSE', 'FILL_BLANK', 'MATCHING', 'ESSAY')),
    options JSONB DEFAULT '[]',
    correct_answer TEXT,
    correct_answers JSONB DEFAULT '[]',
    marks DECIMAL(5,2) DEFAULT 1,
    order_index INT DEFAULT 0,
    explanation TEXT,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE quiz_submissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    quiz_id UUID REFERENCES quizzes(id) ON DELETE CASCADE,
    student_id UUID REFERENCES students(id) ON DELETE CASCADE,
    started_at TIMESTAMP DEFAULT NOW(),
    submitted_at TIMESTAMP,
    score DECIMAL(5,2),
    total_marks DECIMAL(5,2),
    percentage DECIMAL(5,2),
    grade_letter VARCHAR(5),
    status VARCHAR(20) DEFAULT 'IN_PROGRESS' CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'GRADED', 'TIME_UP')),
    attempt_number INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE quiz_answers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    submission_id UUID REFERENCES quiz_submissions(id) ON DELETE CASCADE,
    question_id UUID REFERENCES quiz_questions(id) ON DELETE CASCADE,
    answer TEXT,
    selected_options JSONB DEFAULT '[]',
    is_correct BOOLEAN,
    marks_obtained DECIMAL(5,2) DEFAULT 0,
    feedback TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(submission_id, question_id)
);

CREATE INDEX idx_quizzes_school ON quizzes(school_id);
CREATE INDEX idx_quiz_questions_quiz ON quiz_questions(quiz_id);
CREATE INDEX idx_quiz_submissions_quiz ON quiz_submissions(quiz_id);
CREATE INDEX idx_quiz_submissions_student ON quiz_submissions(student_id);
CREATE INDEX idx_quiz_answers_submission ON quiz_answers(submission_id);

CREATE TRIGGER update_quizzes_updated_at BEFORE UPDATE ON quizzes
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
