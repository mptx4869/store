package com.example.store.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.model.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByTitle(String title);

    Page<Book> findByDeletedAtIsNull(Pageable pageable);

    @Query("""
            SELECT b.id        AS id,
                   b.title     AS title,
                   b.imageUrl  AS imageUrl,
                   b.basePrice AS basePrice,
                   b.createdAt AS createdAt
            FROM Book b
            WHERE b.deletedAt IS NULL
            """)
    Slice<BookListRow> findListByDeletedAtIsNull(Pageable pageable);

    @Query("""
            SELECT b.id        AS id,
                   b.title     AS title,
                   b.imageUrl  AS imageUrl,
                   b.basePrice AS basePrice,
                   b.createdAt AS createdAt
            FROM Book b
            WHERE b.id IN :ids
              AND b.deletedAt IS NULL
            """)
    List<BookListRow> findListByIds(@Param("ids") List<Long> ids);

    @Query("""
            SELECT b FROM Book b
            WHERE b.deletedAt IS NULL
              AND (
                (b.createdAt IS NOT NULL AND b.createdAt >= :cutoffDateTime)
                OR (b.publishedDate IS NOT NULL AND b.publishedDate >= :cutoffDate)
              )
            """)
    Page<Book> findNewBooks(@Param("cutoffDate") LocalDate cutoffDate,
                            @Param("cutoffDateTime") LocalDateTime cutoffDateTime,
                            Pageable pageable);

    @Query("""
            SELECT b.id        AS id,
                   b.title     AS title,
                   b.imageUrl  AS imageUrl,
                   b.basePrice AS basePrice,
                   b.createdAt AS createdAt
            FROM Book b
            WHERE b.deletedAt IS NULL
              AND (
                (b.createdAt IS NOT NULL AND b.createdAt >= :cutoffDateTime)
                OR (b.publishedDate IS NOT NULL AND b.publishedDate >= :cutoffDate)
              )
            """)
    Slice<BookListRow> findNewBookList(@Param("cutoffDate") LocalDate cutoffDate,
                                       @Param("cutoffDateTime") LocalDateTime cutoffDateTime,
                                       Pageable pageable);

    // -------------------------------------------------------------------------
    // Search — fallback (trigram LIKE, uses idx_books_title_trgm +
    //          idx_authors_full_name_trgm from V6, idx_book_authors_author_id from V7)
    // -------------------------------------------------------------------------

    /**
     * Full-entity fallback search (used by internal/recommendation callers).
     * Replaced correlated EXISTS with LEFT JOIN + DISTINCT so the GIN trigram
     * indexes on title and author full_name are reachable by the query planner.
     */
    @Query(value = """
            SELECT DISTINCT b FROM Book b
            LEFT JOIN b.bookAuthors ba
            LEFT JOIN ba.author a
            WHERE b.deletedAt IS NULL
              AND (
                LOWER(b.title)     LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(DISTINCT b.id) FROM Book b
            LEFT JOIN b.bookAuthors ba
            LEFT JOIN ba.author a
            WHERE b.deletedAt IS NULL
              AND (
                LOWER(b.title)     LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """)
    Page<Book> searchBooksFallback(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Projection-only fallback search used by BookService.searchBooks.
     * Native query gives precise column selection (avoids loading description, etc.).
     * published_date is included in SELECT so DISTINCT + ORDER BY published_date works.
     * mapSearchPageable in BookService converts camelCase sort props to snake_case
     * before this query receives the Pageable.
     */
    @Query(
            value = """
                    SELECT DISTINCT
                           b.id,
                           b.title,
                           b.image_url    AS imageUrl,
                           b.base_price   AS basePrice,
                           b.created_at   AS createdAt,
                           b.published_date
                    FROM books b
                    LEFT JOIN book_authors ba ON ba.book_id = b.id
                    LEFT JOIN authors      a  ON a.id = ba.author_id
                    WHERE b.deleted_at IS NULL
                      AND (
                        LOWER(b.title)      LIKE LOWER(CONCAT('%', :keyword, '%'))
                        OR LOWER(a.full_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                      )
                    """,
            nativeQuery = true
    )
    Slice<BookListRow> searchBookListFallback(@Param("keyword") String keyword, Pageable pageable);

    // -------------------------------------------------------------------------
    // Search — full-text (PostgreSQL tsvector/tsquery, uses GIN FTS indexes from V2
    //          and idx_book_authors_author_id from V7)
    // -------------------------------------------------------------------------

    /**
     * Full-entity full-text search.
     * LEFT JOIN replaces correlated EXISTS so both GIN FTS indexes are accessible.
     */
    @Query(
            value = """
                    SELECT DISTINCT b.* FROM books b
                    LEFT JOIN book_authors ba ON ba.book_id = b.id
                    LEFT JOIN authors      a  ON a.id = ba.author_id
                    WHERE b.deleted_at IS NULL
                      AND (
                        to_tsvector('simple', coalesce(b.title,       '')) @@ plainto_tsquery('simple', :keyword)
                        OR to_tsvector('simple', coalesce(a.full_name, '')) @@ plainto_tsquery('simple', :keyword)
                      )
                    """,
            countQuery = """
                    SELECT COUNT(DISTINCT b.id) FROM books b
                    LEFT JOIN book_authors ba ON ba.book_id = b.id
                    LEFT JOIN authors      a  ON a.id = ba.author_id
                    WHERE b.deleted_at IS NULL
                      AND (
                        to_tsvector('simple', coalesce(b.title,       '')) @@ plainto_tsquery('simple', :keyword)
                        OR to_tsvector('simple', coalesce(a.full_name, '')) @@ plainto_tsquery('simple', :keyword)
                      )
                    """,
            nativeQuery = true
    )
    Page<Book> searchBooksFullText(@Param("keyword") String keyword, Pageable pageable);

    /**
     * Projection-only full-text search used by BookService.searchBooks.
     * Slice return type avoids the count query entirely — the extra COUNT(*) on
     * 1M+ rows is expensive and unnecessary for infinite-scroll / next-page UIs.
     */
    @Query(
            value = """
                    SELECT DISTINCT
                           b.id,
                           b.title,
                           b.image_url    AS imageUrl,
                           b.base_price   AS basePrice,
                           b.created_at   AS createdAt,
                           b.published_date
                    FROM books b
                    LEFT JOIN book_authors ba ON ba.book_id = b.id
                    LEFT JOIN authors      a  ON a.id = ba.author_id
                    WHERE b.deleted_at IS NULL
                      AND (
                        to_tsvector('simple', coalesce(b.title,       '')) @@ plainto_tsquery('simple', :keyword)
                        OR to_tsvector('simple', coalesce(a.full_name, '')) @@ plainto_tsquery('simple', :keyword)
                      )
                    """,
            nativeQuery = true
    )
    Slice<BookListRow> searchBookListFullText(@Param("keyword") String keyword, Pageable pageable);

    // -------------------------------------------------------------------------
    // Admin book list — 3 JPQL variants replace the old findAdminBookListWithCount.
    //
    // Rationale: the old query used CASE WHEN :sortBy = 'x' THEN column END
    // which PostgreSQL cannot map to any index, forcing a filesort over the full
    // result set on every request at 1M+ books.
    //
    // Passing Pageable to a JPQL @Query causes Spring Data JPA to emit a plain
    // ORDER BY <field> clause that the query planner CAN use with an index.
    // COUNT(*) OVER() is replaced by Spring Data's automatic count query on Page.
    // -------------------------------------------------------------------------

    /**
     * Active books only (deleted_at IS NULL) — the default admin list view.
     * titlePattern must be pre-built as "%" + title.toLowerCase() + "%" or null.
     */
    @Query(value = """
            SELECT b.id          AS id,
                   b.title       AS title,
                   b.imageUrl    AS imageUrl,
                   b.basePrice   AS basePrice,
                   b.createdAt   AS createdAt,
                   b.deletedAt   AS deletedAt
            FROM Book b
            WHERE (:bookId IS NULL OR b.id = :bookId)
              AND (:titlePattern IS NULL OR LOWER(b.title) LIKE :titlePattern)
              AND b.deletedAt IS NULL
            """,
            countQuery = """
            SELECT COUNT(b) FROM Book b
            WHERE (:bookId IS NULL OR b.id = :bookId)
              AND (:titlePattern IS NULL OR LOWER(b.title) LIKE :titlePattern)
              AND b.deletedAt IS NULL
            """)
    Page<AdminBookListRow> findAdminBookListActive(
            @Param("bookId") Long bookId,
            @Param("titlePattern") String titlePattern,
            Pageable pageable);

    /**
     * All books regardless of soft-delete status (includeDeleted=true).
     * titlePattern must be pre-built as "%" + title.toLowerCase() + "%" or null.
     */
    @Query(value = """
            SELECT b.id          AS id,
                   b.title       AS title,
                   b.imageUrl    AS imageUrl,
                   b.basePrice   AS basePrice,
                   b.createdAt   AS createdAt,
                   b.deletedAt   AS deletedAt
            FROM Book b
            WHERE (:bookId IS NULL OR b.id = :bookId)
              AND (:titlePattern IS NULL OR LOWER(b.title) LIKE :titlePattern)
            """,
            countQuery = """
            SELECT COUNT(b) FROM Book b
            WHERE (:bookId IS NULL OR b.id = :bookId)
              AND (:titlePattern IS NULL OR LOWER(b.title) LIKE :titlePattern)
            """)
    Page<AdminBookListRow> findAdminBookListAll(
            @Param("bookId") Long bookId,
            @Param("titlePattern") String titlePattern,
            Pageable pageable);

    /**
     * Soft-deleted books only (deletedOnly=true).
     * titlePattern must be pre-built as "%" + title.toLowerCase() + "%" or null.
     */
    @Query(value = """
            SELECT b.id          AS id,
                   b.title       AS title,
                   b.imageUrl    AS imageUrl,
                   b.basePrice   AS basePrice,
                   b.createdAt   AS createdAt,
                   b.deletedAt   AS deletedAt
            FROM Book b
            WHERE (:bookId IS NULL OR b.id = :bookId)
              AND (:titlePattern IS NULL OR LOWER(b.title) LIKE :titlePattern)
              AND b.deletedAt IS NOT NULL
            """,
            countQuery = """
            SELECT COUNT(b) FROM Book b
            WHERE (:bookId IS NULL OR b.id = :bookId)
              AND (:titlePattern IS NULL OR LOWER(b.title) LIKE :titlePattern)
              AND b.deletedAt IS NOT NULL
            """)
    Page<AdminBookListRow> findAdminBookListDeletedOnly(
            @Param("bookId") Long bookId,
            @Param("titlePattern") String titlePattern,
            Pageable pageable);

    // -------------------------------------------------------------------------
    // Admin book list — keyset next-page variants
    //
    // Cursor params (lastCreatedAt, lastId) are ALWAYS non-null here.
    // The first page is handled by the caller using the offset-based methods.
    //
    // Root cause of the lower(bytea) bug: passing a null LocalDateTime via
    // setObject(index, null) gives PostgreSQL OID 0 (unknown), which breaks
    // type inference for every other ? in the query — including String LIKE
    // patterns — causing them to be treated as bytea.
    //
    // titlePattern must be pre-built as "%" + title.toLowerCase() + "%", or null.
    // Sort is hardcoded as ORDER BY b.createdAt DESC, b.id DESC.
    // Pass an unsorted Pageable (PageRequest.of(0, size)).
    // -------------------------------------------------------------------------

    /**
     * Active books only — keyset next-page (cursor always non-null).
     */
    @Query("""
            SELECT b.id        AS id,
                   b.title     AS title,
                   b.imageUrl  AS imageUrl,
                   b.basePrice AS basePrice,
                   b.createdAt AS createdAt,
                   b.deletedAt AS deletedAt
            FROM Book b
            WHERE (:bookId IS NULL OR b.id = :bookId)
              AND (:titlePattern IS NULL OR LOWER(b.title) LIKE :titlePattern)
              AND b.deletedAt IS NULL
              AND (b.createdAt < :lastCreatedAt
                   OR (b.createdAt = :lastCreatedAt AND b.id < :lastId))
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    Slice<AdminBookListRow> findAdminBookListActiveKeysetNext(
            @Param("bookId") Long bookId,
            @Param("titlePattern") String titlePattern,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable);

    /**
     * All books — keyset next-page (cursor always non-null).
     */
    @Query("""
            SELECT b.id        AS id,
                   b.title     AS title,
                   b.imageUrl  AS imageUrl,
                   b.basePrice AS basePrice,
                   b.createdAt AS createdAt,
                   b.deletedAt AS deletedAt
            FROM Book b
            WHERE (:bookId IS NULL OR b.id = :bookId)
              AND (:titlePattern IS NULL OR LOWER(b.title) LIKE :titlePattern)
              AND (b.createdAt < :lastCreatedAt
                   OR (b.createdAt = :lastCreatedAt AND b.id < :lastId))
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    Slice<AdminBookListRow> findAdminBookListAllKeysetNext(
            @Param("bookId") Long bookId,
            @Param("titlePattern") String titlePattern,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable);

    /**
     * Soft-deleted books only — keyset next-page (cursor always non-null).
     */
    @Query("""
            SELECT b.id        AS id,
                   b.title     AS title,
                   b.imageUrl  AS imageUrl,
                   b.basePrice AS basePrice,
                   b.createdAt AS createdAt,
                   b.deletedAt AS deletedAt
            FROM Book b
            WHERE (:bookId IS NULL OR b.id = :bookId)
              AND (:titlePattern IS NULL OR LOWER(b.title) LIKE :titlePattern)
              AND b.deletedAt IS NOT NULL
              AND (b.createdAt < :lastCreatedAt
                   OR (b.createdAt = :lastCreatedAt AND b.id < :lastId))
            ORDER BY b.createdAt DESC, b.id DESC
            """)
    Slice<AdminBookListRow> findAdminBookListDeletedOnlyKeysetNext(
            @Param("bookId") Long bookId,
            @Param("titlePattern") String titlePattern,
            @Param("lastCreatedAt") LocalDateTime lastCreatedAt,
            @Param("lastId") Long lastId,
            Pageable pageable);

    // -------------------------------------------------------------------------
    // Projections
    // -------------------------------------------------------------------------

    interface BookListRow {
        Long getId();
        String getTitle();
        String getImageUrl();
        BigDecimal getBasePrice();
        LocalDateTime getCreatedAt();
    }

    interface AdminBookListRow {
        Long getId();
        String getTitle();
        String getImageUrl();
        BigDecimal getBasePrice();
        LocalDateTime getCreatedAt();
        LocalDateTime getDeletedAt();
    }
}
