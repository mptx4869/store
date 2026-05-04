package com.example.store.repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.model.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByTitle(String title);
    
    Page<Book> findByDeletedAtIsNull(Pageable pageable);

    @Query("""
        SELECT b.id AS id,
               b.title AS title,
               b.imageUrl AS imageUrl,
               b.basePrice AS basePrice,
               b.createdAt AS createdAt
        FROM Book b
        WHERE b.deletedAt IS NULL
        """)
    Page<BookListRow> findListByDeletedAtIsNull(Pageable pageable);

    @Query("""
        SELECT b.id AS id,
               b.title AS title,
               b.imageUrl AS imageUrl,
               b.basePrice AS basePrice,
               b.createdAt AS createdAt
        FROM Book b
        WHERE b.id IN :ids
          AND b.deletedAt IS NULL
        """)
    java.util.List<BookListRow> findListByIds(@Param("ids") java.util.List<Long> ids);

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
        SELECT b.id AS id,
               b.title AS title,
               b.imageUrl AS imageUrl,
               b.basePrice AS basePrice,
               b.createdAt AS createdAt
        FROM Book b
        WHERE b.deletedAt IS NULL
          AND (
              (b.createdAt IS NOT NULL AND b.createdAt >= :cutoffDateTime)
              OR (b.publishedDate IS NOT NULL AND b.publishedDate >= :cutoffDate)
          )
        """)
    Page<BookListRow> findNewBookList(@Param("cutoffDate") LocalDate cutoffDate,
                                      @Param("cutoffDateTime") LocalDateTime cutoffDateTime,
                                      Pageable pageable);

    @Query("""
        SELECT b FROM Book b
        WHERE b.deletedAt IS NULL
          AND (
              LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
              OR EXISTS (
                  SELECT 1 FROM BookAuthor ba
                  JOIN ba.author a
                  WHERE ba.book = b
                    AND LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
          )
        """)
    Page<Book> searchBooksFallback(@Param("keyword") String keyword, Pageable pageable);

        @Query("""
                SELECT b.id AS id,
                             b.title AS title,
                             b.imageUrl AS imageUrl,
                             b.basePrice AS basePrice,
                             b.createdAt AS createdAt
                FROM Book b
                WHERE b.deletedAt IS NULL
                    AND (
                            LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            OR EXISTS (
                                    SELECT 1 FROM BookAuthor ba
                                    JOIN ba.author a
                                    WHERE ba.book = b
                                        AND LOWER(a.fullName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                            )
                    )
                """)
        Page<BookListRow> searchBookListFallback(@Param("keyword") String keyword, Pageable pageable);

    @Query(
        value = """
            SELECT b.* FROM books b
            WHERE b.deleted_at IS NULL
              AND (
                  to_tsvector('simple', coalesce(b.title, '')) @@ plainto_tsquery('simple', :keyword)
                  OR EXISTS (
                      SELECT 1 FROM book_authors ba
                      JOIN authors a ON a.id = ba.author_id
                      WHERE ba.book_id = b.id
                        AND to_tsvector('simple', coalesce(a.full_name, '')) @@ plainto_tsquery('simple', :keyword)
                  )
              )
            """,
        countQuery = """
            SELECT COUNT(*) FROM books b
            WHERE b.deleted_at IS NULL
              AND (
                  to_tsvector('simple', coalesce(b.title, '')) @@ plainto_tsquery('simple', :keyword)
                  OR EXISTS (
                      SELECT 1 FROM book_authors ba
                      JOIN authors a ON a.id = ba.author_id
                      WHERE ba.book_id = b.id
                        AND to_tsvector('simple', coalesce(a.full_name, '')) @@ plainto_tsquery('simple', :keyword)
                  )
              )
            """,
        nativeQuery = true
    )
    Page<Book> searchBooksFullText(@Param("keyword") String keyword, Pageable pageable);

    @Query(
        value = """
            SELECT b.id AS id,
                   b.title AS title,
                   b.image_url AS imageUrl,
                   b.base_price AS basePrice,
                   b.created_at AS createdAt
            FROM books b
            WHERE b.deleted_at IS NULL
              AND (
                  to_tsvector('simple', coalesce(b.title, '')) @@ plainto_tsquery('simple', :keyword)
                  OR EXISTS (
                      SELECT 1 FROM book_authors ba
                      JOIN authors a ON a.id = ba.author_id
                      WHERE ba.book_id = b.id
                        AND to_tsvector('simple', coalesce(a.full_name, '')) @@ plainto_tsquery('simple', :keyword)
                  )
              )
            """,
        countQuery = """
            SELECT COUNT(*) FROM books b
            WHERE b.deleted_at IS NULL
              AND (
                  to_tsvector('simple', coalesce(b.title, '')) @@ plainto_tsquery('simple', :keyword)
                  OR EXISTS (
                      SELECT 1 FROM book_authors ba
                      JOIN authors a ON a.id = ba.author_id
                      WHERE ba.book_id = b.id
                        AND to_tsvector('simple', coalesce(a.full_name, '')) @@ plainto_tsquery('simple', :keyword)
                  )
              )
            """,
        nativeQuery = true
    )
    Page<BookListRow> searchBookListFullText(@Param("keyword") String keyword, Pageable pageable);

    @Query("""
        SELECT b.id AS id,
               b.title AS title,
               b.subtitle AS subtitle,
               b.description AS description,
               b.language AS language,
               b.pages AS pages,
               b.publishedDate AS publishedDate,
               b.imageUrl AS imageUrl,
               b.basePrice AS basePrice,
               b.defaultSkuId AS defaultSkuId,
               b.createdAt AS createdAt,
               b.updatedAt AS updatedAt,
               b.deletedAt AS deletedAt
        FROM Book b
        """)
    Page<AdminBookListRow> findAdminBookList(Pageable pageable);

    @Query("""
        SELECT b.id AS id,
               b.title AS title,
               b.subtitle AS subtitle,
               b.description AS description,
               b.language AS language,
               b.pages AS pages,
               b.publishedDate AS publishedDate,
               b.imageUrl AS imageUrl,
               b.basePrice AS basePrice,
               b.defaultSkuId AS defaultSkuId,
               b.createdAt AS createdAt,
               b.updatedAt AS updatedAt,
               b.deletedAt AS deletedAt
        FROM Book b
        WHERE b.deletedAt IS NULL
        """)
    Page<AdminBookListRow> findAdminBookListByDeletedAtIsNull(Pageable pageable);

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

        String getSubtitle();

        String getDescription();

        String getLanguage();

        Integer getPages();

        LocalDate getPublishedDate();

        String getImageUrl();

        BigDecimal getBasePrice();

        Long getDefaultSkuId();

        LocalDateTime getCreatedAt();

        LocalDateTime getUpdatedAt();

        LocalDateTime getDeletedAt();
    }
}
