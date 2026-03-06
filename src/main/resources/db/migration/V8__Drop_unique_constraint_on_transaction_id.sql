-- Drop unique constraint on transaction_id in purchases table
-- Cart checkout creates multiple purchase records with the same transaction_id
ALTER TABLE purchases DROP CONSTRAINT IF EXISTS purchases_transaction_id_key;
