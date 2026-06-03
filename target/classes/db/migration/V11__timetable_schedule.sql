-- =====================================================
-- Timetable / Schedule Management
-- =====================================================

CREATE TABLE timetable_periods (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    name VARCHAR(50) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    period_order INT DEFAULT 0,
    is_break BOOLEAN DEFAULT FALSE,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE timetable_entries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    class_id UUID REFERENCES classes(id) ON DELETE CASCADE,
    subject_id UUID REFERENCES subjects(id),
    teacher_id UUID REFERENCES teachers(id),
    period_id UUID REFERENCES timetable_periods(id),
    day_of_week INT NOT NULL CHECK (day_of_week BETWEEN 0 AND 6),
    room VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(school_id, class_id, day_of_week, period_id)
);

CREATE INDEX idx_timetable_periods_school ON timetable_periods(school_id);
CREATE INDEX idx_timetable_entries_school ON timetable_entries(school_id);
CREATE INDEX idx_timetable_entries_class ON timetable_entries(class_id);
CREATE INDEX idx_timetable_entries_teacher ON timetable_entries(teacher_id);
CREATE INDEX idx_timetable_entries_day ON timetable_entries(class_id, day_of_week);

CREATE TRIGGER update_timetable_entries_updated_at BEFORE UPDATE ON timetable_entries
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();
