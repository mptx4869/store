
-- Add total_orders to users and backfill
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS total_orders INTEGER DEFAULT 0;

UPDATE users u
SET total_orders = COALESCE(o.cnt, 0)
FROM (
    SELECT user_id, COUNT(*) AS cnt
    FROM orders
    GROUP BY user_id
) o
WHERE u.id = o.user_id;
