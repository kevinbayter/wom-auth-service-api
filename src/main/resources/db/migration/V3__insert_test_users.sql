-- V3__insert_test_users.sql
-- Migration to insert test users for development

-- Password: Test123!
-- BCrypt hash generated with strength 10
INSERT INTO users (email, username, password_hash, status, created_at, updated_at) VALUES
('admin@test.com', 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('user@test.com', 'testuser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('locked@test.com', 'lockeduser', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'LOCKED', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Update locked user to have failed attempts and locked_until
UPDATE users 
SET failed_attempts = 5, 
    locked_until = CURRENT_TIMESTAMP + INTERVAL '30 minutes'
WHERE username = 'lockeduser';
