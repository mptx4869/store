package com.example.store.dto;

import java.time.LocalDateTime;
import java.util.List;

import com.example.store.model.Category;

public record AdminCategoryResponse(
    Long id,
    String name,
    String description,
    Long bookCount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt,
    List<CategoryBookInfo> books
) {
    public static AdminCategoryResponse fromEntity(Category category) {
        return new AdminCategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            null, // Will be populated by service when needed
            category.getCreatedAt(),
            category.getUpdatedAt(),
            null  // Books list only for detail view
        );
    }
    
    public static AdminCategoryResponse fromEntityWithBookCount(Category category, Long bookCount) {
        return new AdminCategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            bookCount,
            category.getCreatedAt(),
            category.getUpdatedAt(),
            null
        );
    }
    
    public static AdminCategoryResponse fromEntityWithBooks(Category category, List<CategoryBookInfo> books) {
        Long bookCount = books != null ? (long) books.size() : 0L;
        return new AdminCategoryResponse(
            category.getId(),
            category.getName(),
            category.getDescription(),
            bookCount,
            category.getCreatedAt(),
            category.getUpdatedAt(),
            books
        );
    }
    
    public record CategoryBookInfo(
        Long id,
        String title
    ) {}
}
