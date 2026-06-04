-- =====================================================
-- Payment Gateway Configuration
-- =====================================================

CREATE TABLE IF NOT EXISTS school_payment_gateway_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    school_id UUID NOT NULL UNIQUE REFERENCES schools(id) ON DELETE CASCADE,
    paystack_secret_key VARCHAR(255),
    paystack_public_key VARCHAR(255),
    flutterwave_secret_key VARCHAR(255),
    flutterwave_public_key VARCHAR(255),
    active_gateway VARCHAR(20) NOT NULL DEFAULT 'PAYSTACK',
    fallback_enabled BOOLEAN NOT NULL DEFAULT false,
    paystack_enabled BOOLEAN NOT NULL DEFAULT false,
    flutterwave_enabled BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_spgc_school_id ON school_payment_gateway_configs(school_id);

-- NOTE: permission seeding moved to PermissionInitializer.java
