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