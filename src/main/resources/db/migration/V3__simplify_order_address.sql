-- V3: Simplify address management - remove address reference, store address directly in orders
-- Drop foreign key columns and add address text columns
-- Drop addresses table as it's no longer needed

ALTER TABLE orders DROP COLUMN IF EXISTS shipping_address_id;
ALTER TABLE orders DROP COLUMN IF EXISTS billing_address_id;

ALTER TABLE orders ADD COLUMN shipping_address VARCHAR(500);
ALTER TABLE orders ADD COLUMN shipping_phone VARCHAR(20);
ALTER TABLE orders ADD COLUMN billing_address VARCHAR(500);
ALTER TABLE orders ADD COLUMN billing_phone VARCHAR(20);

-- Drop addresses table
DROP TABLE IF EXISTS addresses;
