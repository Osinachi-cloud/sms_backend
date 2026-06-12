-- =====================================================
-- ID Card Generation
-- =====================================================

CREATE TABLE IF NOT EXISTS id_card_templates (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(100) NOT NULL,
    layout_config JSONB DEFAULT '{}',
    front_design JSONB DEFAULT '{}',
    back_design JSONB DEFAULT '{}',
    is_default BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS student_id_cards (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    student_id UUID REFERENCES students(id) ON DELETE CASCADE,
    template_id UUID REFERENCES id_card_templates(id),
    card_number VARCHAR(50) NOT NULL,
    issue_date DATE DEFAULT CURRENT_DATE,
    expiry_date DATE,
    qr_code TEXT,
    generated_pdf_url TEXT,
    status VARCHAR(20) DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'LOST', 'EXPIRED', 'REVOKED')),
    created_by UUID REFERENCES users(id),
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(school_id, card_number)
);

CREATE INDEX IF NOT EXISTS idx_id_card_templates_school ON id_card_templates(school_id);
CREATE INDEX IF NOT EXISTS idx_student_id_cards_school ON student_id_cards(school_id);
CREATE INDEX IF NOT EXISTS idx_student_id_cards_student ON student_id_cards(student_id);

DO $$
BEGIN
    CREATE TRIGGER update_id_card_templates_updated_at BEFORE UPDATE ON id_card_templates
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;

DO $$
BEGIN
    CREATE TRIGGER update_student_id_cards_updated_at BEFORE UPDATE ON student_id_cards
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
