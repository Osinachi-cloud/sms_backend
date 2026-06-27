-- Add username support to users table
-- Students can log in with username OR email; other users require email

ALTER TABLE users ADD COLUMN username VARCHAR(255) UNIQUE;

-- Make email nullable so students without emails can use usernames instead.
-- The existing UNIQUE constraint on email works with NULLs in PostgreSQL
-- (NULL != NULL, so multiple rows can have NULL email).
ALTER TABLE users ALTER COLUMN email DROP NOT NULL;

-- Add case-insensitive unique index for username lookups
CREATE UNIQUE INDEX idx_users_username_lower ON users (LOWER(username)) WHERE username IS NOT NULL;
