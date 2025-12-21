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
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Book;
import com.example.store.model.Inventory;
import com.example.store.model.ProductSku;
import com.example.store.model.Publisher;
import com.example.store.repository.BookRepository;
import com.example.store.repository.InventoryRepository;
import com.example.store.repository.ProductSkuRepository;
import com.example.store.repository.PublisherRepository;

@Service
public class AdminBookService {

    @Autowired
    private BookRepository bookRepository;
    
    @Autowired
    private ProductSkuRepository productSkuRepository;
    
    @Autowired
    private InventoryRepository inventoryRepository;
    
    @Autowired
    private PublisherRepository publisherRepository;

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
        // Validate publisher if provided
        Publisher publisher = null;
        if (request.getPublisherId() != null) {
            publisher = publisherRepository.findById(request.getPublisherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher not found with id: " + request.getPublisherId()));
        }
        
        // Create book
        Book book = Book.builder()
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .description(request.getDescription())
                .language(request.getLanguage())
                .pages(request.getPages())
                .publisher(publisher)
                .publishedDate(request.getPublishedDate())
                .basePrice(request.getBasePrice())
                .build();
        
        book = bookRepository.save(book);
        
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
        
        // Validate publisher if provided
        if (request.getPublisherId() != null) {
            Publisher publisher = publisherRepository.findById(request.getPublisherId())
                    .orElseThrow(() -> new ResourceNotFoundException("Publisher not found with id: " + request.getPublisherId()));
            book.setPublisher(publisher);
        }
        
        // Update fields
        book.setTitle(request.getTitle());
        book.setSubtitle(request.getSubtitle());
        book.setDescription(request.getDescription());
        book.setLanguage(request.getLanguage());
        book.setPages(request.getPages());
        book.setPublishedDate(request.getPublishedDate());
        book.setBasePrice(request.getBasePrice());
        
        book = bookRepository.save(book);
        
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
        
        return AdminBookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .subtitle(book.getSubtitle())
                .description(book.getDescription())
                .language(book.getLanguage())
                .pages(book.getPages())
                .publisherName(book.getPublisher() != null ? book.getPublisher().getName() : null)
                .publisherId(book.getPublisher() != null ? book.getPublisher().getId() : null)
                .publishedDate(book.getPublishedDate())
                .basePrice(book.getBasePrice())
                .defaultSkuId(book.getDefaultSkuId())
                .createdAt(book.getCreatedAt())
                .updatedAt(book.getUpdatedAt())
                .deletedAt(book.getDeletedAt())
                .skus(skuInfos)
                .build();
    }
}
