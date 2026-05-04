-- Performance optimization indexes

-- Composite index for listing and sorting books efficiently
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_books_deleted_created ON books(deleted_at, created_at DESC);

-- Index for ProductSKU foreign key
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_product_skus_book_id ON product_skus(book_id);

-- Index for BookCategories foreign key
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_book_categories_book_id ON book_categories(book_id);
