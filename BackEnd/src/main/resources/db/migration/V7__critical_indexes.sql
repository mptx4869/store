-- flyway:executeInTransaction=false
-- Critical indexes missing from V1-V6, causing full table scans on 1M+ records

-- orders.user_id: required by getUserById stats, getUserHistory, admin user list JOIN
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_user_id
    ON orders(user_id);

-- Composite (user_id, created_at DESC): covers ORDER BY in findByUserIdOrderByCreatedAtDesc
-- and the LIMIT 5 recent-orders query in getUserById
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_orders_user_id_created
    ON orders(user_id, created_at DESC);

-- book_authors.author_id: required by LEFT JOIN in all book search queries
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_book_authors_author_id
    ON book_authors(author_id);

-- users.role_id: required by findByRoleName and admin user list JOIN on roles
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_role_id
    ON users(role_id);

-- users.status: required by findByStatus filter in admin user list
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_status
    ON users(status);

-- Composite (role_id, status): covers findByRoleNameAndStatus (both filters applied together)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_role_status
    ON users(role_id, status);
