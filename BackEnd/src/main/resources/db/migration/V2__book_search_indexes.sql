-- Indexes for book listing and full-text search

CREATE INDEX IF NOT EXISTS idx_books_created_at ON books (created_at);
CREATE INDEX IF NOT EXISTS idx_books_published_date ON books (published_date);
CREATE INDEX IF NOT EXISTS idx_books_deleted_at ON books (deleted_at);

-- Full-text search indexes (Postgres)
CREATE INDEX IF NOT EXISTS idx_books_title_fts
    ON books USING GIN (to_tsvector('simple', coalesce(title, '')));

CREATE INDEX IF NOT EXISTS idx_authors_full_name_fts
    ON authors USING GIN (to_tsvector('simple', coalesce(full_name, '')));
