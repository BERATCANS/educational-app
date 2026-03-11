-- V12: Safe Delete (Soft Delete) kolonlarının tüm kritik tablolara eklenmesi
-- İlişkilerin bozulmaması ve geçmiş finansal / eğitimsel verilerin silinmemesi için.

-- 1. Tablolara is_deleted kolonu ekleme
ALTER TABLE users ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE books ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE publishers ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE topics ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE tests ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE questions ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE daily_questions ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE discount_codes ADD COLUMN is_deleted BOOLEAN DEFAULT FALSE NOT NULL;

-- 2. Performans (Arama) için index oluşturulması
-- Bu indexler Hibernate'in @SQLRestriction("is_deleted = false") sorgularını hızlandıracak
CREATE INDEX idx_users_is_deleted ON users(is_deleted);
CREATE INDEX idx_books_is_deleted ON books(is_deleted);
CREATE INDEX idx_topics_is_deleted ON topics(is_deleted);
CREATE INDEX idx_tests_is_deleted ON tests(is_deleted);
CREATE INDEX idx_questions_is_deleted ON questions(is_deleted);
CREATE INDEX idx_daily_questions_is_deleted ON daily_questions(is_deleted);
CREATE INDEX idx_discount_codes_is_deleted ON discount_codes(is_deleted);
