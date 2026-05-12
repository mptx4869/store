-- flyway:executeInTransaction=false
-- Composite indexes supporting keyset (cursor) pagination on admin list endpoints.
-- Sort key is always (created_at DESC, id DESC) — id is the stable tiebreaker.

-- books keyset
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_created_at_id
    ON books(created_at DESC, id DESC);

-- users keyset
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_created_at_id
    ON users(created_at DESC, id DESC);
