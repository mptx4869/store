package com.example.store.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.model.Category;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Category findByName(String name);
    
    Optional<Category> findByNameIgnoreCase(String name);
    
    boolean existsByNameIgnoreCase(String name);

    @Query(
        value = """
            SELECT c.id AS id,
                   c.name AS name,
                   c.description AS description,
                   c.createdAt AS createdAt,
                   c.updatedAt AS updatedAt,
                   COUNT(bc) AS bookCount
            FROM Category c
            LEFT JOIN c.bookCategories bc
            GROUP BY c.id, c.name, c.description, c.createdAt, c.updatedAt
            """,
        countQuery = "SELECT COUNT(c) FROM Category c"
    )
    Page<AdminCategoryRow> findAdminCategoryPage(Pageable pageable);
    
    @Query("SELECT COUNT(bc) FROM BookCategory bc WHERE bc.category.id = :categoryId")
    Long countBooksByCategoryId(@Param("categoryId") Long categoryId);

    interface AdminCategoryRow {
        Long getId();

        String getName();

        String getDescription();

        Long getBookCount();

        LocalDateTime getCreatedAt();

        LocalDateTime getUpdatedAt();
    }
}
