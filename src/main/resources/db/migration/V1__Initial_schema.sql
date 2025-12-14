-- Flyway Migration Script
-- V1__Initial_schema.sql
-- 
-- This schema is generated based on JPA entities only
-- Clean schema matching the current codebase

-- ============================================
-- TABLES
-- ============================================

-- Users table
CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) NOT NULL,
    username VARCHAR(30) NOT NULL,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(50) NOT NULL,
    grade_level VARCHAR(50),
    exam_type VARCHAR(50),
    field_type VARCHAR(50),
    gender VARCHAR(50),
    city VARCHAR(50),
    school_type VARCHAR(50),
    daily_question_target INT NOT NULL DEFAULT 200,
    daily_solved_questions INT NOT NULL DEFAULT 0,
    current_streak INT NOT NULL DEFAULT 0,
    longest_streak INT NOT NULL DEFAULT 0,
    last_activity_date DATE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    email_verification_token VARCHAR(100),
    email_verified_at TIMESTAMP,
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT uk_users_phone_number UNIQUE (phone_number),
    CONSTRAINT uk_users_username UNIQUE (username)
);

-- Indexes for users
CREATE INDEX IF NOT EXISTS idx_user_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_user_username ON users(username);

-- Publishers table
CREATE TABLE IF NOT EXISTS publishers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL UNIQUE,
    description TEXT
);

-- Books table
CREATE TABLE IF NOT EXISTS books (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    publisher_id BIGINT,
    description TEXT,
    grade_level VARCHAR(50),
    exam_type VARCHAR(50),
    lesson_type VARCHAR(50),
    cover_image_url VARCHAR(500),
    paid BOOLEAN NOT NULL DEFAULT FALSE,
    price NUMERIC(10, 2),
    number_of_topics INT NOT NULL DEFAULT 0,
    number_of_tests INT NOT NULL DEFAULT 0,
    number_of_questions INT NOT NULL DEFAULT 0,
    average_rating REAL NOT NULL DEFAULT 0.0,
    review_count BIGINT NOT NULL DEFAULT 0,
    FOREIGN KEY (publisher_id) REFERENCES publishers(id)
);

-- Indexes for books
CREATE INDEX IF NOT EXISTS idx_book_exam_type ON books(exam_type);
CREATE INDEX IF NOT EXISTS idx_book_grade_level ON books(grade_level);
CREATE INDEX IF NOT EXISTS idx_book_paid ON books(paid);
CREATE INDEX IF NOT EXISTS idx_book_price ON books(price);

-- Topics table
CREATE TABLE IF NOT EXISTS topics (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    book_id BIGINT,
    number_of_tests INT NOT NULL DEFAULT 0,
    number_of_questions INT NOT NULL DEFAULT 0,
    sequence_number INT,
    FOREIGN KEY (book_id) REFERENCES books(id)
);

-- Tests table
CREATE TABLE IF NOT EXISTS tests (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    topic_id BIGINT,
    number_of_questions INT NOT NULL DEFAULT 0,
    sequence_number INT,
    FOREIGN KEY (topic_id) REFERENCES topics(id)
);

-- Questions table
CREATE TABLE IF NOT EXISTS questions (
    id BIGSERIAL PRIMARY KEY,
    test_id BIGINT NOT NULL,
    image_url VARCHAR(500) NOT NULL,
    answer_number INT NOT NULL DEFAULT 5,
    correct_answer VARCHAR(50) NOT NULL,
    explanation_text TEXT,
    explanation_likes INT,
    explanation_dislikes INT,
    video_url VARCHAR(500),
    sequence_number INT,
    exam_type VARCHAR(50),
    ai_analysis_status VARCHAR(50) DEFAULT 'PENDING',
    FOREIGN KEY (test_id) REFERENCES tests(id)
);

-- Question hints collection table
CREATE TABLE IF NOT EXISTS question_hints (
    question_id BIGINT NOT NULL,
    text TEXT,
    likes INT,
    dislikes INT,
    sequence_number INT,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Book codes table
CREATE TABLE IF NOT EXISTS book_codes (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    used BOOLEAN NOT NULL DEFAULT FALSE,
    book_id BIGINT NOT NULL,
    user_id BIGINT,
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- User-Book join table (Many-to-Many)
CREATE TABLE IF NOT EXISTS user_books (
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, book_id),
    CONSTRAINT uk_user_book UNIQUE (user_id, book_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- User-Flagged Questions join table (Many-to-Many)
CREATE TABLE IF NOT EXISTS user_flagged_questions (
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, question_id),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
);

-- Discount codes table (carts'tan önce oluşturulmalı)
CREATE TABLE IF NOT EXISTS discount_codes (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(500) NOT NULL,
    discount_type VARCHAR(50) NOT NULL,
    discount_value NUMERIC(10, 2) NOT NULL,
    usage_limit INT NOT NULL,
    used_count INT NOT NULL DEFAULT 0,
    min_cart_total NUMERIC(10, 2),
    max_cart_total NUMERIC(10, 2),
    valid_from TIMESTAMP NOT NULL,
    valid_until TIMESTAMP NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    publisher_id BIGINT,
    max_usage_per_user INT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (publisher_id) REFERENCES publishers(id)
);

-- Indexes for discount_codes
CREATE INDEX IF NOT EXISTS idx_discount_code_code ON discount_codes(code);
CREATE INDEX IF NOT EXISTS idx_discount_code_active ON discount_codes(is_active);

-- Carts table
CREATE TABLE IF NOT EXISTS carts (
    id BIGSERIAL PRIMARY KEY,
    version BIGINT NOT NULL,
    user_id BIGINT NOT NULL UNIQUE,
    discount_code_id BIGINT,
    discount_amount NUMERIC(10, 2),
    total_price_after_discount NUMERIC(10, 2),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (discount_code_id) REFERENCES discount_codes(id)
);

-- Cart items table
CREATE TABLE IF NOT EXISTS cart_items (
    id BIGSERIAL PRIMARY KEY,
    cart_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    price_at_add NUMERIC(10, 2) NOT NULL,
    added_at TIMESTAMP NOT NULL,
    FOREIGN KEY (cart_id) REFERENCES carts(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id)
);

-- Purchases table
CREATE TABLE IF NOT EXISTS purchases (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    price NUMERIC(10, 2) NOT NULL,
    discount_code_id BIGINT,
    discount_amount NUMERIC(10, 2),
    final_price NUMERIC(10, 2),
    purchase_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    transaction_id VARCHAR(100) UNIQUE,
    payment_method VARCHAR(50),
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (discount_code_id) REFERENCES discount_codes(id)
);

-- Book reviews table
CREATE TABLE IF NOT EXISTS book_reviews (
    id BIGSERIAL PRIMARY KEY,
    book_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    UNIQUE (book_id, user_id),
    FOREIGN KEY (book_id) REFERENCES books(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Indexes for book_reviews
CREATE INDEX IF NOT EXISTS idx_book_review_book_id ON book_reviews(book_id);
CREATE INDEX IF NOT EXISTS idx_book_review_user_id ON book_reviews(user_id);

-- Test results table
CREATE TABLE IF NOT EXISTS test_results (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    test_id BIGINT NOT NULL,
    score INT NOT NULL DEFAULT 0,
    total_questions INT NOT NULL DEFAULT 0,
    success_rate DOUBLE PRECISION NOT NULL DEFAULT 0.0,
    time_spent_seconds INT,
    completed_at TIMESTAMP NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (test_id) REFERENCES tests(id)
);

-- Indexes for test_results
CREATE INDEX IF NOT EXISTS idx_test_result_user_id ON test_results(user_id);
CREATE INDEX IF NOT EXISTS idx_test_result_test_id ON test_results(test_id);
CREATE INDEX IF NOT EXISTS idx_test_result_completed_at ON test_results(completed_at);

-- Question results table
CREATE TABLE IF NOT EXISTS question_results (
    id BIGSERIAL PRIMARY KEY,
    test_result_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_answer VARCHAR(50),
    is_correct BOOLEAN NOT NULL DEFAULT FALSE,
    time_spent_seconds INT,
    answered_at TIMESTAMP NOT NULL,
    FOREIGN KEY (test_result_id) REFERENCES test_results(id) ON DELETE CASCADE,
    FOREIGN KEY (question_id) REFERENCES questions(id)
);

-- Discount code books join table (Many-to-Many)
CREATE TABLE IF NOT EXISTS discount_code_books (
    discount_code_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    PRIMARY KEY (discount_code_id, book_id),
    FOREIGN KEY (discount_code_id) REFERENCES discount_codes(id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- Discount code usages table
CREATE TABLE IF NOT EXISTS discount_code_usages (
    id BIGSERIAL PRIMARY KEY,
    discount_code_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    used_at TIMESTAMP NOT NULL,
    transaction_id VARCHAR(100),
    FOREIGN KEY (discount_code_id) REFERENCES discount_codes(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Indexes for discount_code_usages
CREATE INDEX IF NOT EXISTS idx_discount_code_usage_code ON discount_code_usages(discount_code_id);
CREATE INDEX IF NOT EXISTS idx_discount_code_usage_user ON discount_code_usages(user_id);
CREATE INDEX IF NOT EXISTS idx_discount_code_usage_date ON discount_code_usages(used_at);

-- Daily questions table
CREATE TABLE IF NOT EXISTS daily_questions (
    id BIGSERIAL PRIMARY KEY,
    image_url VARCHAR(500) NOT NULL,
    answer_number INT NOT NULL,
    correct_answer VARCHAR(50) NOT NULL,
    video_url VARCHAR(500),
    explanation TEXT,
    display_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    active BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL
);

-- Daily question answers table
CREATE TABLE IF NOT EXISTS daily_question_answers (
    id BIGSERIAL PRIMARY KEY,
    daily_question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    selected_answer VARCHAR(50) NOT NULL,
    answered_at TIMESTAMP NOT NULL,
    UNIQUE (daily_question_id, user_id),
    FOREIGN KEY (daily_question_id) REFERENCES daily_questions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Question AI conversations table
CREATE TABLE IF NOT EXISTS question_ai_conversations (
    id BIGSERIAL PRIMARY KEY,
    question_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    last_message_at TIMESTAMP,
    UNIQUE (user_id, question_id),
    FOREIGN KEY (question_id) REFERENCES questions(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
);

-- Question AI messages table
CREATE TABLE IF NOT EXISTS question_ai_messages (
    id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    role VARCHAR(50) NOT NULL,
    message_type VARCHAR(50),
    created_at TIMESTAMP NOT NULL,
    token_count INT,
    FOREIGN KEY (conversation_id) REFERENCES question_ai_conversations(id) ON DELETE CASCADE
);
