-- Fix password hashes for test users
-- The correct BCrypt hash for password "Test123!" with strength 10

UPDATE users 
SET password_hash = '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi'
WHERE email IN ('admin@test.com', 'user@test.com', 'locked@test.com');
