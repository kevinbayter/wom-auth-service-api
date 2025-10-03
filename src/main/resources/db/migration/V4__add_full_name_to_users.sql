-- Add full_name column to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS full_name VARCHAR(100);

-- Update existing users with a default full name
UPDATE users SET full_name = username WHERE full_name IS NULL;
