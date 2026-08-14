-- ============================================
-- UniReview Database Schema (V1 Migration)
-- ============================================

-- 1. Teachers Table
CREATE TABLE teachers (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    title VARCHAR(20),
    faculty VARCHAR(100) NOT NULL,
    department VARCHAR(100),
    avatar_url VARCHAR(255),
    avg_rating NUMERIC(3, 2) DEFAULT 0.00,
    total_reviews INT DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 2. Subjects Table
CREATE TABLE subjects (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(20) UNIQUE NOT NULL,
    name VARCHAR(150) NOT NULL,
    faculty VARCHAR(100),
    credits INT DEFAULT 3,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Teacher Subjects Mapping
CREATE TABLE teacher_subjects (
    id BIGSERIAL PRIMARY KEY,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    subject_id BIGINT NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
    semester VARCHAR(20),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 4. Reviewers Table (Token-based Anonymous Identity)
CREATE TABLE reviewers (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(50) UNIQUE NOT NULL,
    review_count INT DEFAULT 0,
    credit_balance INT DEFAULT 0 CONSTRAINT chk_credit_balance CHECK (credit_balance >= 0),
    ip_hash VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_active_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 5. Reviews Table
CREATE TABLE reviews (
    id BIGSERIAL PRIMARY KEY,
    reviewer_token VARCHAR(50) NOT NULL REFERENCES reviewers(token) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    subject_id BIGINT REFERENCES subjects(id) ON DELETE SET NULL,
    rating_overall INT NOT NULL CHECK (rating_overall BETWEEN 1 AND 5),
    rating_teaching INT NOT NULL CHECK (rating_teaching BETWEEN 1 AND 5),
    rating_grading INT NOT NULL CHECK (rating_grading BETWEEN 1 AND 5),
    rating_personality INT NOT NULL CHECK (rating_personality BETWEEN 1 AND 5),
    difficulty VARCHAR(20) NOT NULL,
    attendance VARCHAR(20) NOT NULL,
    materials_allowed VARCHAR(20) NOT NULL,
    would_recommend VARCHAR(20) NOT NULL,
    workload VARCHAR(20) NOT NULL,
    content TEXT NOT NULL,
    semester VARCHAR(20) NOT NULL,
    upvote_count INT DEFAULT 0,
    downvote_count INT DEFAULT 0,
    status VARCHAR(20) DEFAULT 'PENDING',
    toxicity_score NUMERIC(3, 2),
    ip_hash VARCHAR(64),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reviewer_teacher UNIQUE (reviewer_token, teacher_id)
);

-- 6. Unlocked Teachers Table
CREATE TABLE unlocked_teachers (
    id BIGSERIAL PRIMARY KEY,
    reviewer_token VARCHAR(50) NOT NULL REFERENCES reviewers(token) ON DELETE CASCADE,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE CASCADE,
    unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_reviewer_unlock UNIQUE (reviewer_token, teacher_id)
);

-- 7. Review Votes Table
CREATE TABLE review_votes (
    id BIGSERIAL PRIMARY KEY,
    voter_token VARCHAR(50) NOT NULL REFERENCES reviewers(token) ON DELETE CASCADE,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    vote_type VARCHAR(10) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_voter_review UNIQUE (voter_token, review_id)
);

-- 8. Review Reports Table
CREATE TABLE review_reports (
    id BIGSERIAL PRIMARY KEY,
    review_id BIGINT NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 9. Admin Users Table
CREATE TABLE admin_users (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) DEFAULT 'MODERATOR',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for Query Performance
CREATE INDEX idx_teachers_faculty ON teachers(faculty);
CREATE INDEX idx_teachers_rating ON teachers(avg_rating DESC);
CREATE INDEX idx_reviews_teacher ON reviews(teacher_id);
CREATE INDEX idx_reviews_status ON reviews(status);
CREATE INDEX idx_unlocked_reviewer ON unlocked_teachers(reviewer_token);

-- Initial Admin Account (Password: admin123 hashed using BCrypt)
INSERT INTO admin_users (username, password_hash, role)
VALUES ('admin', '$2a$10$e84WvH2M4nLw4q8.aA0D7.eS3M2N.gX3Q5J2X1X8Y8Z8Y8Z8Y8Z8', 'ADMIN');
