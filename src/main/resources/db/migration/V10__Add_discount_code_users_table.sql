-- Add discount_code_users join table for user-specific discount codes
CREATE TABLE IF NOT EXISTS discount_code_users (
    discount_code_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    PRIMARY KEY (discount_code_id, user_id),
    CONSTRAINT fk_dcu_discount_code FOREIGN KEY (discount_code_id) REFERENCES discount_codes(id) ON DELETE CASCADE,
    CONSTRAINT fk_dcu_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dcu_discount_code_id ON discount_code_users(discount_code_id);
CREATE INDEX IF NOT EXISTS idx_dcu_user_id ON discount_code_users(user_id);
