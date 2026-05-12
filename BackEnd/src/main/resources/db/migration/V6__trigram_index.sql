-- flyway:executeInTransaction=false



CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_title_trgm
    ON books USING GIN (LOWER(title) gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username_trgm
    ON users USING GIN (LOWER(username) gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_email_trgm
    ON users USING GIN (LOWER(email) gin_trgm_ops);

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_authors_full_name_trgm
    ON authors USING GIN (LOWER(full_name) gin_trgm_ops);
