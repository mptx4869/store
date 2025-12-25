package com.example.store.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import com.example.store.dto.BookResponse;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Book;
import com.example.store.model.ProductSku;
import com.example.store.repository.BookRepository;
import com.example.store.repository.ProductSkuRepository;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final ProductSkuRepository productSkuRepository;
 
    public BookService(BookRepository bookRepository, ProductSkuRepository productSkuRepository) {
        this.bookRepository = bookRepository;
        this.productSkuRepository = productSkuRepository;
    }

    public List<BookResponse> getAllBooks() {
        return bookRepository.findAll()
            .stream()
            .map(this::mapToResponse)
            .toList();
    }
 
    public BookResponse getBookById(Long id) {
        return bookRepository.findById(id)
            .map(this::mapToResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    private BookResponse mapToResponse(Book book) {
        ProductSku defaultSku = resolveDefaultSku(book);
        BigDecimal price = defaultSku != null ? defaultSku.resolveCurrentPrice() : book.getBasePrice();
        
        // Get all SKUs for this book
        List<ProductSku> allSkus = productSkuRepository.findByBookId(book.getId());
        List<BookResponse.SkuInfo> skuInfos = allSkus.stream()
            .map(sku -> mapToSkuInfo(sku, book.getDefaultSkuId()))
            .toList();

        return BookResponse.builder()
            .id(book.getId())
            .title(book.getTitle())
            .subtitle(book.getSubtitle())
            .publishedDate(book.getPublishedDate())
            .description(book.getDescription())
            .language(book.getLanguage())
            .pages(book.getPages())
            .price(price)
            .sku(defaultSku != null ? defaultSku.getSku() : null)
            .imageUrl(book.getImageUrl())
            .skus(skuInfos)
            .build();
    }
    
    private BookResponse.SkuInfo mapToSkuInfo(ProductSku sku, Long defaultSkuId) {
        int availableStock = 0;
        if (sku.getInventory() != null) {
            availableStock = sku.getInventory().getStock() - sku.getInventory().getReserved();
            availableStock = Math.max(0, availableStock); // Ensure non-negative
        }
        boolean inStock = availableStock > 0;
        boolean isDefault = sku.getId().equals(defaultSkuId);
        
        return BookResponse.SkuInfo.builder()
            .id(sku.getId())
            .sku(sku.getSku())
            .format(sku.getFormat())
            .price(sku.resolveCurrentPrice())
            .inStock(inStock)
            .availableStock(availableStock)
            .isDefault(isDefault)
            .weightGrams(sku.getWeightGrams())
            .lengthMm(sku.getLengthMm())
            .widthMm(sku.getWidthMm())
            .heightMm(sku.getHeightMm())
            .build();
    }

    private ProductSku resolveDefaultSku(Book book) {
        if (book.getDefaultSkuId() != null) {
            return productSkuRepository.findById(book.getDefaultSkuId())
                .orElseGet(() -> productSkuRepository.findFirstByBookId(book.getId()).orElse(null));
        }
        return productSkuRepository.findFirstByBookId(book.getId()).orElse(null);
    }
}