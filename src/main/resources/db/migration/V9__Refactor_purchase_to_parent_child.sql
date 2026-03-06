-- Refactor purchases into parent-child structure (Purchase → PurchaseItem)

-- 1. Create purchase_items table
CREATE TABLE IF NOT EXISTS purchase_items (
    id BIGSERIAL PRIMARY KEY,
    purchase_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    price DECIMAL(10, 2) NOT NULL,
    discount_amount DECIMAL(10, 2),
    final_price DECIMAL(10, 2),
    CONSTRAINT fk_purchase_items_purchase FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
    CONSTRAINT fk_purchase_items_book FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE CASCADE
);

-- 2. Migrate existing purchase data to purchase_items
INSERT INTO purchase_items (purchase_id, book_id, price, discount_amount, final_price)
SELECT id, book_id, price, discount_amount, final_price
FROM purchases
WHERE book_id IS NOT NULL;

-- 3. Add new columns to purchases (order-level totals)
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS total_price DECIMAL(10, 2);
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS total_discount DECIMAL(10, 2);
ALTER TABLE purchases ADD COLUMN IF NOT EXISTS item_count INT;

-- 4. Populate new columns from existing data
-- Group by transaction_id to calculate totals per order
UPDATE purchases p SET
    total_price = sub.total_price,
    total_discount = sub.total_discount,
    item_count = sub.item_count
FROM (
    SELECT
        transaction_id,
        SUM(price) as total_price,
        COALESCE(SUM(discount_amount), 0) as total_discount,
        COUNT(*) as item_count
    FROM purchases
    WHERE transaction_id IS NOT NULL
    GROUP BY transaction_id
) sub
WHERE p.transaction_id = sub.transaction_id;

-- Handle purchases without transaction_id (single item purchases)
UPDATE purchases SET
    total_price = COALESCE(price, 0),
    total_discount = COALESCE(discount_amount, 0),
    item_count = 1
WHERE total_price IS NULL;

-- Recalculate final_price at order level
UPDATE purchases SET
    final_price = total_price - COALESCE(total_discount, 0)
WHERE total_price IS NOT NULL;

-- 5. Set NOT NULL constraints now that data is populated
ALTER TABLE purchases ALTER COLUMN total_price SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN final_price SET NOT NULL;
ALTER TABLE purchases ALTER COLUMN item_count SET NOT NULL;

-- 6. Remove old per-book columns from purchases
-- (keeping book_id temporarily nullable for backwards compatibility, will drop later)
ALTER TABLE purchases ALTER COLUMN book_id DROP NOT NULL;
ALTER TABLE purchases ALTER COLUMN price DROP NOT NULL;

-- 7. Remove duplicate purchase rows (keep only one per transaction_id)
-- Since we migrated all items to purchase_items, we need one purchase row per transaction
DELETE FROM purchases p
WHERE p.id NOT IN (
    SELECT MIN(id)
    FROM purchases
    GROUP BY COALESCE(transaction_id, id::text)
);

-- 8. Now drop the old per-book columns
ALTER TABLE purchases DROP COLUMN IF EXISTS book_id;
ALTER TABLE purchases DROP COLUMN IF EXISTS price;
ALTER TABLE purchases DROP COLUMN IF EXISTS discount_amount;

-- 9. Create index for performance
CREATE INDEX IF NOT EXISTS idx_purchase_items_purchase_id ON purchase_items(purchase_id);
CREATE INDEX IF NOT EXISTS idx_purchase_items_book_id ON purchase_items(book_id);
