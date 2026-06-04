-- =====================================================
-- Smart Onboarding / Tooltip System
-- =====================================================

CREATE TABLE onboarding_steps (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    step_key VARCHAR(100) UNIQUE NOT NULL,
    target_page VARCHAR(100) NOT NULL,
    target_selector VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    position VARCHAR(20) DEFAULT 'bottom' CHECK (position IN ('top', 'bottom', 'left', 'right', 'center')),
    step_order INT DEFAULT 0,
    target_roles TEXT[] DEFAULT '{}',
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE user_onboarding_progress (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    step_key VARCHAR(100) NOT NULL,
    is_completed BOOLEAN DEFAULT FALSE,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT NOW(),
    UNIQUE(user_id, step_key)
);

CREATE INDEX idx_onboarding_steps_page ON onboarding_steps(target_page);
CREATE INDEX idx_onboarding_steps_order ON onboarding_steps(target_page, step_order);
CREATE INDEX idx_user_onboarding_user ON user_onboarding_progress(user_id);
