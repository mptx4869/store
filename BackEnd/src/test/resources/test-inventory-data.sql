-- Test inventory data for all ProductSku records
-- This ensures stock validation works correctly in tests

-- Insert inventory records using ProductSku IDs
-- Note: These will be inserted after ProductSku entities are created in test setup
-- Using MERGE to handle both insert and update scenarios

MERGE INTO inventory (sku_id, stock, reserved, last_updated) 
KEY (sku_id)
SELECT ps.id, 100, 0, CURRENT_TIMESTAMP
FROM product_sku ps
WHERE NOT EXISTS (SELECT 1 FROM inventory i WHERE i.sku_id = ps.id);
