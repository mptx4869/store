-- Test data for AdminInventoryControllerTest
-- Creates book, SKU, and inventory only
-- Users and roles are created via Java code for proper password hashing

-- Insert test book
INSERT INTO books (id, title, base_price, created_at, updated_at) VALUES
(100, 'Test Book', 29.99, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Insert test SKU
INSERT INTO product_skus (id, book_id, sku, format, created_at, updated_at) VALUES
(100, 100, 'TEST-SKU-001', 'PAPERBACK', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Update book with default SKU
UPDATE books SET default_sku_id = 100 WHERE id = 100;

-- Insert inventory with low stock
INSERT INTO inventory (sku_id, stock, reserved, last_updated) VALUES
(100, 5, 0, CURRENT_TIMESTAMP);
