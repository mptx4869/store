package com.example.store.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.AdminBookResponse;
import com.example.store.dto.BookCreateRequest;
import com.example.store.dto.CursorPageResponse;
import com.example.store.dto.BookUpdateRequest;
import com.example.store.dto.SkuCreateRequest;
import com.example.store.dto.SkuUpdateRequest;
import com.example.store.exception.ConflictException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Book;
import com.example.store.model.Category;
import com.example.store.model.Inventory;
import com.example.store.model.ProductSku;
import com.example.store.repository.BookCategoryRepository;
import com.example.store.repository.BookRepository;
import com.example.store.repository.CategoryRepository;
import com.example.store.repository.InventoryRepository;
import com.example.store.repository.ProductSkuRepository;

@Service
public class AdminBookService {

    @Autowired
    private BookRepository bookRepository;

    @Autowired
    private BookCategoryRepository bookCategoryRepository;

    @Autowired
    private ProductSkuRepository productSkuRepository;

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    /**
     * Paginated admin book list.
     *
     * Three JPQL query variants replace the old findAdminBookListWithCount which
     * used CASE WHEN :sortBy = 'x' THEN column END ORDER BY — an expression that
     * PostgreSQL cannot map to any index, causing filesort over the full result set
     * at 1M+ books.  Passing Pageable to each JPQL variant lets Spring Data emit a
     * plain ORDER BY clause, and the COUNT(*) OVER() window function is replaced by
     * Spring Data's automatic secondary count query on Page return type.
     */
    @Transactional(readOnly = true)
    public Page<AdminBookResponse> getAllBooks(
            Pageable pageable,
            Boolean includeDeleted,
            Boolean deletedOnly,
            Long bookId,
            String title) {

        String titlePattern = (title != null && !title.isBlank())
                ? ("%" + title.trim().toLowerCase() + "%")
                : null;
        boolean onlyDeleted = Boolean.TRUE.equals(deletedOnly);
        boolean includeAll  = Boolean.TRUE.equals(includeDeleted);
        if (onlyDeleted) {
            includeAll = false;
        }

        Pageable normalizedPageable = normalizeBookPageable(pageable);

        Page<BookRepository.AdminBookListRow> rows;
        if (onlyDeleted) {
            rows = bookRepository.findAdminBookListDeletedOnly(bookId, titlePattern, normalizedPageable);
        } else if (includeAll) {
            rows = bookRepository.findAdminBookListAll(bookId, titlePattern, normalizedPageable);
        } else {
            rows = bookRepository.findAdminBookListActive(bookId, titlePattern, normalizedPageable);
        }

        if (rows.isEmpty()) {
            return rows.map(book -> mapToAdminBookListResponse(book, List.of()));
        }

        List<Long> bookIds = rows.getContent().stream()
                .map(BookRepository.AdminBookListRow::getId)
                .toList();

        Map<Long, List<AdminBookResponse.CategoryInfo>> categoriesByBookId = loadCategoryInfo(bookIds);
        return rows.map(book -> mapToAdminBookListResponse(
                book,
                categoriesByBookId.getOrDefault(book.getId(), List.of())));
    }

    /**
     * Keyset (cursor) variant of the admin book list.
     *
     * Sort is always {@code created_at DESC, id DESC}.  Pass {@code lastId}
     * and {@code lastCreatedAt} from the previous response to continue paging;
     * omit both for the first page.
     */
    @Transactional(readOnly = true)
    public CursorPageResponse<AdminBookResponse> getAllBooksCursor(
            int size,
            Long lastId,
            LocalDateTime lastCreatedAt,
            Boolean includeDeleted,
            Boolean deletedOnly,
            Long bookId,
            String title) {

        boolean onlyDeleted = Boolean.TRUE.equals(deletedOnly);
        boolean includeAll  = Boolean.TRUE.equals(includeDeleted) && !onlyDeleted;

        List<AdminBookResponse> content;
        boolean hasNext;

        if (lastId == null || lastCreatedAt == null) {
            // First page: reuse the offset-based query — no LocalDateTime param,
            // so no null-timestamp/bytea type-inference problem.
            Pageable firstPage = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<AdminBookResponse> page = getAllBooks(firstPage, includeDeleted, deletedOnly, bookId, title);
            content = page.getContent();
            hasNext = page.hasNext();
        } else {
            // Subsequent pages: cursor is always non-null — safe to pass LocalDateTime.
            // Pre-build LIKE pattern to avoid CONCAT type-inference bug (lower(bytea)).
            String titlePattern = (title != null && !title.isBlank())
                    ? ("%" + title.trim().toLowerCase() + "%")
                    : null;

            Pageable pageable = PageRequest.of(0, size);

            Slice<BookRepository.AdminBookListRow> slice;
            if (onlyDeleted) {
                slice = bookRepository.findAdminBookListDeletedOnlyKeysetNext(
                        bookId, titlePattern, lastCreatedAt, lastId, pageable);
            } else if (includeAll) {
                slice = bookRepository.findAdminBookListAllKeysetNext(
                        bookId, titlePattern, lastCreatedAt, lastId, pageable);
            } else {
                slice = bookRepository.findAdminBookListActiveKeysetNext(
                        bookId, titlePattern, lastCreatedAt, lastId, pageable);
            }

            List<BookRepository.AdminBookListRow> rows = slice.getContent();
            List<Long> bookIds = rows.stream().map(BookRepository.AdminBookListRow::getId).toList();
            Map<Long, List<AdminBookResponse.CategoryInfo>> categoriesByBookId =
                    bookIds.isEmpty() ? Map.of() : loadCategoryInfo(bookIds);

            content = rows.stream()
                    .map(b -> mapToAdminBookListResponse(
                            b, categoriesByBookId.getOrDefault(b.getId(), List.of())))
                    .toList();
            hasNext = slice.hasNext();
        }

        Long nextLastId = null;
        LocalDateTime nextLastCreatedAt = null;
        if (hasNext && !content.isEmpty()) {
            AdminBookResponse last = content.get(content.size() - 1);
            nextLastId = last.getId();
            nextLastCreatedAt = last.getCreatedAt();
        }

        return CursorPageResponse.<AdminBookResponse>builder()
                .content(content)
                .hasNext(hasNext)
                .nextLastId(nextLastId)
                .nextLastCreatedAt(nextLastCreatedAt)
                .build();
    }

    @Transactional(readOnly = true)
    public AdminBookResponse getBookById(Long bookId) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));
        return mapToAdminBookResponse(book);
    }

    @Transactional
    public AdminBookResponse createBook(BookCreateRequest request) {
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

        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            updateBookCategories(book, request.getCategoryIds());
        }

        Long defaultSkuId = null;
        for (BookCreateRequest.SkuCreateRequest skuRequest : request.getSkus()) {
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

            int initialStock = skuRequest.getInitialStock() != null ? skuRequest.getInitialStock() : 0;
            Inventory inventory = new Inventory();
            inventory.setProductSku(sku);
            inventory.setStock(initialStock);
            inventory.setReserved(0);
            inventoryRepository.save(inventory);

            if (Boolean.TRUE.equals(skuRequest.getIsDefault()) || defaultSkuId == null) {
                defaultSkuId = sku.getId();
            }
        }

        book.setDefaultSkuId(defaultSkuId);
        book = bookRepository.save(book);
        return mapToAdminBookResponse(book);
    }

    @Transactional
    public AdminBookResponse updateBook(Long bookId, BookUpdateRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        if (book.getDeletedAt() != null) {
            throw new IllegalStateException("Cannot update deleted book");
        }

        book.setTitle(request.getTitle());
        book.setSubtitle(request.getSubtitle());
        book.setDescription(request.getDescription());
        book.setLanguage(request.getLanguage());
        book.setPages(request.getPages());
        book.setPublishedDate(request.getPublishedDate());
        book.setImageUrl(request.getImageUrl());
        book.setBasePrice(request.getBasePrice());
        book = bookRepository.save(book);

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

    private Map<Long, List<AdminBookResponse.CategoryInfo>> loadCategoryInfo(List<Long> bookIds) {
        Map<Long, List<AdminBookResponse.CategoryInfo>> categoriesByBookId = new HashMap<>();
        if (bookIds.isEmpty()) {
            return categoriesByBookId;
        }

        List<BookCategoryRepository.CategoryRow> rows = bookCategoryRepository.findCategoryRowsByBookIdIn(bookIds);
        for (BookCategoryRepository.CategoryRow row : rows) {
            Long bookId = row.getBookId();
            AdminBookResponse.CategoryInfo info = AdminBookResponse.CategoryInfo.builder()
                    .id(row.getCategoryId())
                    .name(row.getCategoryName())
                    .build();
            categoriesByBookId
                    .computeIfAbsent(bookId, key -> new ArrayList<>())
                    .add(info);
        }
        return categoriesByBookId;
    }

    private AdminBookResponse mapToAdminBookListResponse(
            BookRepository.AdminBookListRow book,
            List<AdminBookResponse.CategoryInfo> categories) {
        return AdminBookResponse.builder()
                .id(book.getId())
                .title(book.getTitle())
                .imageUrl(book.getImageUrl())
                .basePrice(book.getBasePrice())
                .createdAt(book.getCreatedAt())
                .deletedAt(book.getDeletedAt())
                .categories(categories)
                .skus(List.of())
                .build();
    }

    private AdminBookResponse mapToAdminBookResponse(Book book) {
        // findByBookId has @EntityGraph({"inventory"}) — inventory is already loaded
        // in each SKU; no extra query per SKU is needed.
        List<ProductSku> skus = productSkuRepository.findByBookId(book.getId());

        List<AdminBookResponse.SkuInfo> skuInfos = skus.stream()
                .map(sku -> {
                    Inventory inventory = sku.getInventory();

                    int stock     = inventory != null ? inventory.getStock()    : 0;
                    int reserved  = inventory != null ? inventory.getReserved() : 0;
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

        List<AdminBookResponse.CategoryInfo> categoryInfos = bookCategoryRepository
                .findCategoryRowsByBookIdIn(List.of(book.getId())).stream()
                .map(row -> AdminBookResponse.CategoryInfo.builder()
                        .id(row.getCategoryId())
                        .name(row.getCategoryName())
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

    // -------------------------------------------------------------------------
    // SKU Management
    // -------------------------------------------------------------------------

    @Transactional
    public AdminBookResponse addSku(Long bookId, SkuCreateRequest request) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with id: " + bookId));

        if (book.getDeletedAt() != null) {
            throw new IllegalStateException("Cannot add SKU to deleted book");
        }

        if (productSkuRepository.findBySku(request.getSku()).isPresent()) {
            throw new ConflictException("SKU already exists: " + request.getSku());
        }

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

        int initialStock = request.getInitialStock() != null ? request.getInitialStock() : 0;
        Inventory inventory = new Inventory();
        inventory.setProductSku(sku);
        inventory.setStock(initialStock);
        inventory.setReserved(0);
        inventoryRepository.save(inventory);

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

        productSkuRepository.findBySku(request.getSku())
                .ifPresent(existingSku -> {
                    if (!existingSku.getId().equals(skuId)) {
                        throw new ConflictException("SKU already exists: " + request.getSku());
                    }
                });

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

        productSkuRepository.findById(skuId)
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

        List<ProductSku> allSkus = productSkuRepository.findByBookId(bookId);
        if (allSkus.size() <= 1) {
            throw new ConflictException("Cannot delete the last SKU. Book must have at least one SKU.");
        }

        if (sku.getId().equals(book.getDefaultSkuId())) {
            ProductSku newDefaultSku = allSkus.stream()
                    .filter(s -> !s.getId().equals(skuId))
                    .findFirst()
                    .orElseThrow();
            book.setDefaultSkuId(newDefaultSku.getId());
            bookRepository.save(book);
        }

        inventoryRepository.findByProductSkuId(skuId).ifPresent(inventoryRepository::delete);
        productSkuRepository.delete(sku);
        return mapToAdminBookResponse(book);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void updateBookCategories(Book book, List<Long> categoryIds) {
        bookCategoryRepository.deleteByBookId(book.getId());

        if (categoryIds != null && !categoryIds.isEmpty()) {
            List<Category> categories = categoryRepository.findAllById(categoryIds);
            Map<Long, Category> categoryMap = new HashMap<>();
            for (Category category : categories) {
                categoryMap.put(category.getId(), category);
            }

            List<com.example.store.model.BookCategory> links = new ArrayList<>();
            for (Long categoryId : categoryIds) {
                Category category = categoryMap.get(categoryId);
                if (category == null) {
                    throw new ResourceNotFoundException("Category not found with id: " + categoryId);
                }
                links.add(com.example.store.model.BookCategory.builder()
                        .book(book)
                        .category(category)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            bookCategoryRepository.saveAll(links);
        }
    }

    /**
     * Rebuilds a Pageable with validated sort properties so that an unknown field
     * from the request cannot reach JPQL and cause a query parsing error.
     * Valid properties map directly to Book entity field names.
     */
    private Pageable normalizeBookPageable(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            return pageable;
        }
        List<Sort.Order> normalized = pageable.getSort().stream()
                .map(o -> {
                    String prop = switch (o.getProperty()) {
                        case "id"        -> "id";
                        case "title"     -> "title";
                        case "basePrice" -> "basePrice";
                        default          -> "createdAt";
                    };
                    return new Sort.Order(o.getDirection(), prop);
                })
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(normalized));
    }
}
