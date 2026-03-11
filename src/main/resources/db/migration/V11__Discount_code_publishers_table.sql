-- Create discount_code_publishers join table (ManyToMany: discount_codes <-> publishers)
CREATE TABLE IF NOT EXISTS discount_code_publishers (
    discount_code_id BIGINT NOT NULL,
    publisher_id BIGINT NOT NULL,
    PRIMARY KEY (discount_code_id, publisher_id),
    CONSTRAINT fk_dcp_discount_code FOREIGN KEY (discount_code_id) REFERENCES discount_codes(id) ON DELETE CASCADE,
    CONSTRAINT fk_dcp_publisher FOREIGN KEY (publisher_id) REFERENCES publishers(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_dcp_discount_code_id ON discount_code_publishers(discount_code_id);
CREATE INDEX IF NOT EXISTS idx_dcp_publisher_id ON discount_code_publishers(publisher_id);

-- Migrate existing publisher_id data from discount_codes to the new join table
INSERT INTO discount_code_publishers (discount_code_id, publisher_id)
SELECT id, publisher_id FROM discount_codes WHERE publisher_id IS NOT NULL;

-- Drop old publisher_id column from discount_codes
ALTER TABLE discount_codes DROP COLUMN IF EXISTS publisher_id;
