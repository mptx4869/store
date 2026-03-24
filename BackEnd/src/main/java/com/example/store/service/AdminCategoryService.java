package com.example.store.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.AdminCategoryResponse;
import com.example.store.dto.AdminCategoryResponse.CategoryBookInfo;
import com.example.store.dto.CategoryCreateRequest;
import com.example.store.dto.CategoryUpdateRequest;
import com.example.store.exception.ConflictException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Category;
import com.example.store.repository.CategoryRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    public Page<AdminCategoryResponse> getAllCategories(int page, int size, String sortBy, String sortDirection) {
        Sort.Direction direction = "ASC".equalsIgnoreCase(sortDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        
        Page<Category> categories = categoryRepository.findAllWithBookCount(pageable);
        
        return categories.map(category -> {
            Long bookCount = categoryRepository.countBooksByCategoryId(category.getId());
            return AdminCategoryResponse.fromEntityWithBookCount(category, bookCount);
        });
    }

    @Transactional(readOnly = true)
    public AdminCategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        List<CategoryBookInfo> books = category.getBookCategories().stream()
            .map(bc -> new CategoryBookInfo(
                bc.getBook().getId(),
                bc.getBook().getTitle()
            ))
            .collect(Collectors.toList());
        
        return AdminCategoryResponse.fromEntityWithBooks(category, books);
    }

    @Transactional
    public AdminCategoryResponse createCategory(CategoryCreateRequest request) {
        // Check for duplicate name
        if (categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Category with name '" + request.name() + "' already exists");
        }
        
        Category category = Category.builder()
            .name(request.name())
            .description(request.description())
            .build();
        
        Category saved = categoryRepository.save(category);
        return AdminCategoryResponse.fromEntity(saved);
    }

    @Transactional
    public AdminCategoryResponse updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        // Check for duplicate name (excluding current category)
        if (!category.getName().equalsIgnoreCase(request.name()) && 
            categoryRepository.existsByNameIgnoreCase(request.name())) {
            throw new ConflictException("Category with name '" + request.name() + "' already exists");
        }
        
        category.setName(request.name());
        category.setDescription(request.description());
        
        Category updated = categoryRepository.save(category);
        return AdminCategoryResponse.fromEntity(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + id));
        
        // Check if category has books
        Long bookCount = categoryRepository.countBooksByCategoryId(id);
        if (bookCount > 0) {
            throw new ConflictException("Cannot delete category with " + bookCount + " associated book(s)");
        }
        
        categoryRepository.delete(category);
    }
}
