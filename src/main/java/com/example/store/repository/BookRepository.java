package com.example.store.repository;

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
}
