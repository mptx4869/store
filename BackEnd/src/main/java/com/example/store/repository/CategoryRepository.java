package com.example.store.repository;

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
    
    @Query("SELECT c FROM Category c LEFT JOIN c.bookCategories bc GROUP BY c.id ORDER BY c.createdAt DESC")
    Page<Category> findAllWithBookCount(Pageable pageable);
    
    @Query("SELECT COUNT(bc) FROM BookCategory bc WHERE bc.category.id = :categoryId")
    Long countBooksByCategoryId(@Param("categoryId") Long categoryId);
}
