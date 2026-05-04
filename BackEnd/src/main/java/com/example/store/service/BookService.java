package com.example.store.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.example.store.dto.BookListResponse;
import com.example.store.dto.BookResponse;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Book;
import com.example.store.model.ProductSku;
import com.example.store.repository.BookCategoryRepository;
import com.example.store.repository.BookRepository;
import com.example.store.repository.ProductSkuRepository;

@Service
public class BookService {

    private final BookRepository bookRepository;
    private final ProductSkuRepository productSkuRepository;
    private final BookCategoryRepository bookCategoryRepository;

    @Value("${book.new.threshold-days:30}")
    private int newThresholdDays;

    @Value("${book.search.fulltext.enabled:true}")
    private boolean fullTextSearchEnabled;
 
    public BookService(
            BookRepository bookRepository,
            ProductSkuRepository productSkuRepository,
            BookCategoryRepository bookCategoryRepository) {
        this.bookRepository = bookRepository;
        this.productSkuRepository = productSkuRepository;
        this.bookCategoryRepository = bookCategoryRepository;
    }

    public Page<BookListResponse> getBooks(Pageable pageable) {
        Pageable cappedPageable = capPageSize(pageable, 50);
        Page<BookRepository.BookListRow> books = bookRepository.findListByDeletedAtIsNull(cappedPageable);
        return mapToListPage(books);
    }

    public Page<BookListResponse> getNewBooks(Pageable pageable) {
        // Cap page size to prevent abuse
        Pageable cappedPageable = capPageSize(pageable, 50);
        
        // Calculate cutoff dates
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        LocalDateTime cutoffDateTime = now.minusDays(newThresholdDays);
        LocalDate cutoffDate = cutoffDateTime.toLocalDate();

        Page<BookRepository.BookListRow> books = bookRepository.findNewBookList(
            cutoffDate,
            cutoffDateTime,
            cappedPageable);
        return mapToListPage(books);
    }
 
    public BookResponse getBookById(Long id) {
        return bookRepository.findById(id)
            .map(this::mapToDetailResponse)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
    }

    public Page<BookListResponse> searchBooks(String keyword, Pageable pageable) {
        Pageable cappedPageable = capPageSize(pageable, 50);
        Pageable searchPageable = fullTextSearchEnabled
            ? mapSearchPageable(cappedPageable)
            : cappedPageable;

        Page<BookRepository.BookListRow> books = fullTextSearchEnabled
            ? bookRepository.searchBookListFullText(keyword, searchPageable)
            : bookRepository.searchBookListFallback(keyword, searchPageable);
        return mapToListPage(books);
    }

    public List<BookListResponse> getBooksByIdsOrdered(List<Long> bookIds) {
        if (bookIds == null || bookIds.isEmpty()) {
            return List.of();
        }

        List<BookRepository.BookListRow> books = bookRepository.findListByIds(bookIds);
        Map<Long, BookRepository.BookListRow> bookMap = new HashMap<>();
        for (BookRepository.BookListRow row : books) {
            bookMap.put(row.getId(), row);
        }

        Map<Long, List<BookListResponse.CategoryInfo>> categoriesByBookId = loadCategoryInfo(bookIds);

        List<BookListResponse> result = new ArrayList<>();
        for (Long id : bookIds) {
            BookRepository.BookListRow book = bookMap.get(id);
            if (book != null) {
                result.add(mapToListResponse(book, categoriesByBookId.getOrDefault(id, List.of())));
            }
        }
        return result;
    }

    private Pageable capPageSize(Pageable pageable, int maxSize) {
        if (pageable.getPageSize() > maxSize) {
            return PageRequest.of(pageable.getPageNumber(), maxSize, pageable.getSort());
        }
        return pageable;
    }

    private Pageable mapSearchPageable(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            return pageable;
        }

        List<Sort.Order> mappedOrders = new ArrayList<>();
        for (Sort.Order order : pageable.getSort()) {
            String column = switch (order.getProperty()) {
                case "createdAt" -> "created_at";
                case "publishedDate" -> "published_date";
                case "title" -> "title";
                default -> "created_at";
            };
            mappedOrders.add(new Sort.Order(order.getDirection(), column));
        }

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(mappedOrders));
    }

    private Page<BookListResponse> mapToListPage(Page<BookRepository.BookListRow> books) {
        if (books.isEmpty()) {
            return books.map(book -> mapToListResponse(book, List.of()));
        }

        List<Long> bookIds = books.getContent().stream()
                .map(BookRepository.BookListRow::getId)
                .toList();

        Map<Long, List<BookListResponse.CategoryInfo>> categoriesByBookId = loadCategoryInfo(bookIds);

        return books.map(book -> mapToListResponse(
                book,
                categoriesByBookId.getOrDefault(book.getId(), List.of())));
    }

    private Map<Long, List<BookListResponse.CategoryInfo>> loadCategoryInfo(List<Long> bookIds) {
        Map<Long, List<BookListResponse.CategoryInfo>> categoriesByBookId = new HashMap<>();
        if (bookIds.isEmpty()) {
            return categoriesByBookId;
        }

        List<BookCategoryRepository.CategoryRow> rows = bookCategoryRepository.findCategoryRowsByBookIdIn(bookIds);
        for (BookCategoryRepository.CategoryRow row : rows) {
            Long bookId = row.getBookId();
            BookListResponse.CategoryInfo info = BookListResponse.CategoryInfo.builder()
                    .id(row.getCategoryId())
                    .name(row.getCategoryName())
                    .build();

            categoriesByBookId
                    .computeIfAbsent(bookId, key -> new ArrayList<>())
                    .add(info);
        }

        return categoriesByBookId;
    }

    private BookListResponse mapToListResponse(
            BookRepository.BookListRow book,
            List<BookListResponse.CategoryInfo> categories) {
        return BookListResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .imageUrl(book.getImageUrl())
                .basePrice(book.getBasePrice())
                .createdAt(book.getCreatedAt())
                .categories(categories)
                .build();
    }

    private BookResponse mapToDetailResponse(Book book) {
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