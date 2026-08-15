-- ============================================
-- UniReview Database Schema (V2 Migration)
-- Additive fixes: reporter identity, rate-limit index, admin seed password
-- ============================================

-- 1. Attribute reports to a reviewer identity (FR-016)
ALTER TABLE review_reports ADD COLUMN reporter_token VARCHAR(50) NOT NULL DEFAULT '';
ALTER TABLE review_reports ALTER COLUMN reporter_token DROP DEFAULT;

-- 2. Support the per-IP daily rate-limit query (FR-017)
CREATE INDEX idx_reviews_iphash_created ON reviews(ip_hash, created_at);

-- 3. Fix the malformed seed admin password hash (Password: admin123, valid BCrypt hash)
UPDATE admin_users
SET password_hash = '$2a$10$eXbl6pzQ6qXTnnnn9rOZbenrEQMweiyIQ0TyPH6FkL.bMpI.0rYhi'
WHERE username = 'admin';
