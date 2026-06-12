-- =====================================================
-- Teacher Activity Tracking
-- =====================================================

CREATE TABLE IF NOT EXISTS teacher_activity_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    school_id UUID REFERENCES schools(id) ON DELETE CASCADE,
    teacher_id UUID REFERENCES teachers(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id),
    activity_type VARCHAR(50) NOT NULL CHECK (activity_type IN ('LOGIN', 'CONTENT_CREATED', 'CONTENT_APPROVED', 'GRADE_ENTERED', 'ATTENDANCE_MARKED', 'MESSAGE_SENT', 'QUIZ_CREATED', 'LESSON_PLAN_CREATED', 'FILE_UPLOADED', 'STUDENT_INTERACTION')),
    description TEXT,
    entity_type VARCHAR(50),
    entity_id UUID,
    metadata JSONB DEFAULT '{}',
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_teacher_activity_school ON teacher_activity_logs(school_id);
CREATE INDEX IF NOT EXISTS idx_teacher_activity_teacher ON teacher_activity_logs(teacher_id);
CREATE INDEX IF NOT EXISTS idx_teacher_activity_type ON teacher_activity_logs(school_id, activity_type);
CREATE INDEX IF NOT EXISTS idx_teacher_activity_created ON teacher_activity_logs(created_at);
