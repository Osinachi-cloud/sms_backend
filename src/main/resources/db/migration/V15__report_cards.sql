-- =====================================================
-- Report Card Generation
-- =====================================================

CREATE TABLE report_card_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    layout_config JSONB DEFAULT '{}',
    header_text TEXT,
    footer_text TEXT,
    show_logo BOOLEAN DEFAULT TRUE,
    show_qr_code BOOLEAN DEFAULT FALSE,
    show_attendance BOOLEAN DEFAULT TRUE,
    show_teacher_comments BOOLEAN DEFAULT TRUE,
    show_principal_comment BOOLEAN DEFAULT TRUE,
    grading_scale JSONB DEFAULT '[]',
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE report_cards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    student_id UUID REFERENCES students(id) ON DELETE CASCADE,
    term_id UUID REFERENCES terms(id),
    template_id UUID REFERENCES report_card_templates(id),
    attendance_present INT DEFAULT 0,
    attendance_absent INT DEFAULT 0,
    attendance_late INT DEFAULT 0,
    total_score DECIMAL(5,2),
    average_score DECIMAL(5,2),
    overall_grade VARCHAR(5),
    class_position INT,
    class_size INT,
    teacher_comment TEXT,
    principal_comment TEXT,
    status VARCHAR(20) DEFAULT 'DRAFT' CHECK (status IN ('DRAFT', 'PUBLISHED', 'ARCHIVED')),
    generated_pdf_url TEXT,
    published_at TIMESTAMP,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE report_card_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    report_card_id UUID REFERENCES report_cards(id) ON DELETE CASCADE,
    subject_id UUID REFERENCES subjects(id),
    test_score DECIMAL(5,2),
    exam_score DECIMAL(5,2),
    total_score DECIMAL(5,2),
    grade_letter VARCHAR(5),
    remarks TEXT,
    teacher_id UUID REFERENCES teachers(id),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_report_card_templates_school ON report_card_templates(school_id);
CREATE INDEX idx_report_cards_school ON report_cards(school_id);
CREATE INDEX idx_report_cards_student ON report_cards(student_id);
CREATE INDEX idx_report_cards_term ON report_cards(term_id);
CREATE INDEX idx_report_card_entries_card ON report_card_entries(report_card_id);

CREATE TRIGGER update_report_card_templates_updated_at BEFORE UPDATE ON report_card_templates
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
CREATE TRIGGER update_report_cards_updated_at BEFORE UPDATE ON report_cards
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
