-- =====================================================
-- Online Admission / Application Portal
-- =====================================================

CREATE TABLE IF NOT EXISTS admission_applications (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    application_number VARCHAR(50) UNIQUE NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    gender VARCHAR(20) CHECK (gender IN ('MALE', 'FEMALE', 'OTHER')),
    email VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    previous_school VARCHAR(255),
    last_class_completed VARCHAR(50),
    guardian_name VARCHAR(255),
    guardian_email VARCHAR(255),
    guardian_phone VARCHAR(50),
    guardian_relationship VARCHAR(50),
    intended_class_id UUID REFERENCES classes(id),
    status VARCHAR(20) DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'INTERVIEW_SCHEDULED', 'ACCEPTED', 'REJECTED', 'WAITLISTED', 'ENROLLED')),
    reviewed_by UUID REFERENCES users(id),
    reviewed_at TIMESTAMP,
    review_notes TEXT,
    exam_score DECIMAL(5,2),
    interview_score DECIMAL(5,2),
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS admission_documents (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    application_id UUID REFERENCES admission_applications(id) ON DELETE CASCADE,
    document_type VARCHAR(50) NOT NULL CHECK (document_type IN ('BIRTH_CERTIFICATE', 'TRANSFER_CERTIFICATE', 'REPORT_CARD', 'PASSPORT_PHOTO', 'IDENTITY_PROOF', 'MEDICAL_RECORD', 'OTHER')),
    file_url TEXT NOT NULL,
    file_name VARCHAR(255),
    is_verified BOOLEAN DEFAULT FALSE,
    uploaded_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_admission_applications_school ON admission_applications(school_id);
CREATE INDEX IF NOT EXISTS idx_admission_applications_status ON admission_applications(school_id, status);
CREATE INDEX IF NOT EXISTS idx_admission_documents_app ON admission_documents(application_id);

DO $$
BEGIN
    CREATE TRIGGER update_admission_applications_updated_at BEFORE UPDATE ON admission_applications
        FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
EXCEPTION
    WHEN duplicate_object THEN NULL;
END $$;
