package com.example.store.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.AdminBookResponse;
import com.example.store.dto.BookCreateRequest;
import com.example.store.dto.BookUpdateRequest;
import com.example.store.dto.SkuCreateRequest;
import com.example.store.dto.SkuUpdateRequest;
import com.example.store.exception.ConflictException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Book;
import com.example.store.model.BookCategory;
import com.example.store.model.Category;
import com.example.store.model.Inventory;
import com.example.store.model.ProductSku;
import com.example.store.repository.BookRepository;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.InventoryRepository;
import com.example.store.repository.ProductSkuRepository;

@Service
public class AdminBookService {

    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private ProductSkuRepository productSkuRepository;
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private CategoryRepository categoryRepository;
    
    @Transactional(readOnly = true)
    public Page<AdminBookResponse> getAllBooks(Pageable pageable, Boolean includeDeleted) {
        Page<Book> books;
        if (Boolean.TRUE.equals(includeDeleted)) {
            books = bookRepository.findAll(pageable);
        } else {
            books = bookRepository.findByDeletedAtIsNull(pageable);
        }
        return books.map(this::mapToAdminBookResponse);
    }

    @Transactional(readOnly = true)
    public AdminBookResponse getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        return mapToAdminBookResponse(book);
    }

    @Transactional
    public AdminBookResponse createBook(BookCreateRequest request) {
        // Create book (publisher removed; single image stored on book)
        Book book = Book.builder()
            .title(request.getTitle())
            .subtitle(request.getSubtitle())
            .description(request.getDescription())
            .language(request.getLanguage())
            .pages(request.getPages())
            .publishedDate(request.getPublishedDate())
            .imageUrl(request.getImageUrl())
            .basePrice(request.getBasePrice())
            .build();
        
        book = bookRepository.save(book);
        
        // Add categories
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            updateBookCategories(book, request.getCategoryIds());
        }
        
        // Create SKUs and inventories
        Long defaultSkuId = null;
        for (BookCreateRequest.SkuCreateRequest skuRequest : request.getSkus()) {
            // Check for duplicate SKU
            if (productSkuRepository.findBySku(skuRequest.getSku()).isPresent()) {
                throw new IllegalArgumentException("SKU already exists: " + skuRequest.getSku());
            }
            
            ProductSku sku = ProductSku.builder()
                    .book(book)
                    .sku(skuRequest.getSku())
                    .format(skuRequest.getFormat())
                    .priceOverride(skuRequest.getPriceOverride())
                    .weightGrams(skuRequest.getWeightGrams())
                    .lengthMm(skuRequest.getLengthMm())
                    .widthMm(skuRequest.getWidthMm())
                    .heightMm(skuRequest.getHeightMm())
                    .build();
            
            sku = productSkuRepository.save(sku);
            
            // Create inventory
            int initialStock = skuRequest.getInitialStock() != null ? skuRequest.getInitialStock() : 0;
            Inventory inventory = new Inventory();
            inventory.setProductSku(sku);
            inventory.setStock(initialStock);
            inventory.setReserved(0);
            inventoryRepository.save(inventory);
            
            // Set default SKU
            if (Boolean.TRUE.equals(skuRequest.getIsDefault()) || defaultSkuId == null) {
                defaultSkuId = sku.getId();
            }
        }
        
        // Update book with default SKU
        book.setDefaultSkuId(defaultSkuId);
        book = bookRepository.save(book);
        
        return mapToAdminBookResponse(book);
    }

    @Transactional
    public AdminBookResponse updateBook(Long bookId, BookUpdateRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        
        // Check if book is deleted
        if (book.getDeletedAt() != null) {
            throw new IllegalStateException("Cannot update deleted book");
        }
        
        // Update fields
        book.setTitle(request.getTitle());
        book.setSubtitle(request.getSubtitle());
        book.setDescription(request.getDescription());
        book.setLanguage(request.getLanguage());
        book.setPages(request.getPages());
        book.setPublishedDate(request.getPublishedDate());
        book.setImageUrl(request.getImageUrl());
        book.setBasePrice(request.getBasePrice());
        
        book = bookRepository.save(book);
        
        // Update categories
        if (request.getCategoryIds() != null) {
            updateBookCategories(book, request.getCategoryIds());
        }
        
        return mapToAdminBookResponse(book);
    }

    @Transactional
    public void deleteBook(Long bookId, boolean hardDelete) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        
        if (hardDelete) {
            bookRepository.delete(book);
        } else {
            book.setDeletedAt(LocalDateTime.now());
            bookRepository.save(book);
        }
    }

    @Transactional
    public AdminBookResponse restoreBook(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        if (book.getDeletedAt() == null) {
            throw new ConflictException("Book is not soft deleted");
        }

        book.setDeletedAt(null);
        Book restored = bookRepository.save(book);

        return mapToAdminBookResponse(restored);
    }

    private AdminBookResponse mapToAdminBookResponse(Book book) {
        List<ProductSku> skus = productSkuRepository.findByBookId(book.getId());
        
        List<AdminBookResponse.SkuInfo> skuInfos = skus.stream()
                .map(sku -> {
                    Inventory inventory = inventoryRepository.findByProductSkuId(sku.getId())
                            .orElse(null);
                    
                    int stock = inventory != null ? inventory.getStock() : 0;
                    int reserved = inventory != null ? inventory.getReserved() : 0;
                    int available = stock - reserved;
                    
                    return AdminBookResponse.SkuInfo.builder()
                            .id(sku.getId())
                            .sku(sku.getSku())
                            .format(sku.getFormat())
                            .price(sku.getPriceOverride() != null ? sku.getPriceOverride() : book.getBasePrice())
                            .stock(stock)
                            .reserved(reserved)
                            .available(available)
                            .isDefault(sku.getId().equals(book.getDefaultSkuId()))
                            .build();
                })
                .collect(Collectors.toList());
        
        // Map categories
        List<AdminBookResponse.CategoryInfo> categoryInfos = book.getBookCategories().stream()
                .map(bc -> AdminBookResponse.CategoryInfo.builder()
                        .id(bc.getCategory().getId())
                        .name(bc.getCategory().getName())
                        .build())
                .collect(Collectors.toList());
        
        return AdminBookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .subtitle(book.getSubtitle())
                .description(book.getDescription())
                .language(book.getLanguage())
                .pages(book.getPages())
                .publishedDate(book.getPublishedDate())
                .imageUrl(book.getImageUrl())
                .basePrice(book.getBasePrice())
                .defaultSkuId(book.getDefaultSkuId())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .deletedAt(book.getDeletedAt())
                .categories(categoryInfos)
                .skus(skuInfos)
                .build();
    }

    // SKU Management
    
    @Transactional
    public AdminBookResponse addSku(Long bookId, SkuCreateRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        
        if (book.getDeletedAt() != null) {
            throw new IllegalStateException("Cannot add SKU to deleted book");
        }
        
        // Check duplicate SKU code
        if (productSkuRepository.findBySku(request.getSku()).isPresent()) {
            throw new ConflictException("SKU already exists: " + request.getSku());
        }
        
        // Create new SKU
        ProductSku sku = ProductSku.builder()
                .book(book)
                .sku(request.getSku())
                .format(request.getFormat())
                .priceOverride(request.getPriceOverride())
                .weightGrams(request.getWeightGrams())
                .lengthMm(request.getLengthMm())
                .widthMm(request.getWidthMm())
                .heightMm(request.getHeightMm())
                .build();
        
        sku = productSkuRepository.save(sku);
        
        // Create inventory
        int initialStock = request.getInitialStock() != null ? request.getInitialStock() : 0;
        Inventory inventory = new Inventory();
        inventory.setProductSku(sku);
        inventory.setStock(initialStock);
        inventory.setReserved(0);
        inventoryRepository.save(inventory);
        
        // Set as default if requested or if this is the first SKU
        if (Boolean.TRUE.equals(request.getIsDefault()) || book.getDefaultSkuId() == null) {
            book.setDefaultSkuId(sku.getId());
            bookRepository.save(book);
        }
        
        return mapToAdminBookResponse(book);
    }
    
    @Transactional
    public AdminBookResponse updateSku(Long bookId, Long skuId, SkuUpdateRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        
        ProductSku sku = productSkuRepository.findById(skuId)
                .filter(s -> s.getBook().getId().equals(bookId))
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found for this book"));
        
        // Check duplicate SKU code (except current)
        productSkuRepository.findBySku(request.getSku())
                .ifPresent(existingSku -> {
                    if (!existingSku.getId().equals(skuId)) {
                        throw new ConflictException("SKU already exists: " + request.getSku());
                    }
                });
        
        // Update SKU
        sku.setSku(request.getSku());
        sku.setFormat(request.getFormat());
        sku.setPriceOverride(request.getPriceOverride());
        sku.setWeightGrams(request.getWeightGrams());
        sku.setLengthMm(request.getLengthMm());
        sku.setWidthMm(request.getWidthMm());
        sku.setHeightMm(request.getHeightMm());
        productSkuRepository.save(sku);
        
        return mapToAdminBookResponse(book);
    }
    
    @Transactional
    public AdminBookResponse setDefaultSku(Long bookId, Long skuId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        
        ProductSku sku = productSkuRepository.findById(skuId)
                .filter(s -> s.getBook().getId().equals(bookId))
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found for this book"));
        
        book.setDefaultSkuId(skuId);
        bookRepository.save(book);
        
        return mapToAdminBookResponse(book);
    }
    
    @Transactional
    public AdminBookResponse deleteSku(Long bookId, Long skuId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        
        ProductSku sku = productSkuRepository.findById(skuId)
                .filter(s -> s.getBook().getId().equals(bookId))
                .orElseThrow(() -> new ResourceNotFoundException("SKU not found for this book"));
        
        // Check if this is the last SKU
        List<ProductSku> allSkus = productSkuRepository.findByBookId(bookId);
        if (allSkus.size() <= 1) {
            throw new ConflictException("Cannot delete the last SKU. Book must have at least one SKU.");
        }
        
        // Check if this is the default SKU
        if (sku.getId().equals(book.getDefaultSkuId())) {
            // Set another SKU as default
            ProductSku newDefaultSku = allSkus.stream()
                    .filter(s -> !s.getId().equals(skuId))
                    .findFirst()
                    .orElseThrow();
            book.setDefaultSkuId(newDefaultSku.getId());
            bookRepository.save(book);
        }
        
        // Delete inventory first (foreign key)
        inventoryRepository.findByProductSkuId(skuId).ifPresent(inventoryRepository::delete);
        
        // Delete SKU
        productSkuRepository.delete(sku);
        
        return mapToAdminBookResponse(book);
    }
    
    /**
     * Helper method to update book categories
     */
    private void updateBookCategories(Book book, List<Long> categoryIds) {
        // Clear existing categories
        book.getBookCategories().clear();
        
        // Add new categories
        if (categoryIds != null && !categoryIds.isEmpty()) {
            for (Long categoryId : categoryIds) {
                Category category = categoryRepository.findById(categoryId)
                        .orElseThrow(() -> new ResourceNotFoundException("Category not found with id: " + categoryId));
                
                BookCategory bookCategory = BookCategory.builder()
                        .book(book)
                        .category(category)
                        .createdAt(java.time.LocalDateTime.now())
                        .build();
                
                book.getBookCategories().add(bookCategory);
            }
        }
        
        bookRepository.save(book);
    }
}
