package com.example.store.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.AdminBookResponse;
import com.example.store.dto.BookCreateRequest;
import com.example.store.dto.BookUpdateRequest;
import com.example.store.dto.CursorPageResponse;
import com.example.store.dto.SkuCreateRequest;
import com.example.store.dto.SkuUpdateRequest;
import com.example.store.service.AdminBookService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/books")
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminBookController {

    @Autowired
    private AdminBookService adminBookService;

    @GetMapping
    public ResponseEntity<Page<AdminBookResponse>> getAllBooks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDirection,
            @RequestParam(required = false) Boolean includeDeleted,
            @RequestParam(required = false) Boolean deletedOnly,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String title) {
        
        Sort sort = sortDirection.equalsIgnoreCase("DESC") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AdminBookResponse> books = adminBookService.getAllBooks(
            pageable,
            includeDeleted,
            deletedOnly,
            id,
            title
        );
        
        return ResponseEntity.ok(books);
    }

    /**
     * GET /admin/books/cursor
     * Keyset (cursor) pagination — no COUNT(*) overhead, O(1) per page at any depth.
     *
     * First page: omit lastId and lastCreatedAt.
     * Subsequent pages: pass nextLastId and nextLastCreatedAt from the previous response.
     * Sort is always created_at DESC, id DESC.
     */
    @GetMapping("/cursor")
    public ResponseEntity<CursorPageResponse<AdminBookResponse>> getAllBooksCursor(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreatedAt,
            @RequestParam(required = false) Boolean includeDeleted,
            @RequestParam(required = false) Boolean deletedOnly,
            @RequestParam(required = false) Long id,
            @RequestParam(required = false) String title) {

        int cappedSize = Math.min(Math.max(size, 1), 100);
        CursorPageResponse<AdminBookResponse> response = adminBookService.getAllBooksCursor(
                cappedSize, lastId, lastCreatedAt, includeDeleted, deletedOnly, id, title);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{bookId}")
    public ResponseEntity<AdminBookResponse> getBookById(@PathVariable Long bookId) {
        AdminBookResponse book = adminBookService.getBookById(bookId);
        return ResponseEntity.ok(book);
    }

    @PostMapping
    public ResponseEntity<AdminBookResponse> createBook(@Valid @RequestBody BookCreateRequest request) {
        AdminBookResponse book = adminBookService.createBook(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    @PutMapping("/{bookId}")
    public ResponseEntity<AdminBookResponse> updateBook(
            @PathVariable Long bookId,
            @Valid @RequestBody BookUpdateRequest request) {
        AdminBookResponse book = adminBookService.updateBook(bookId, request);
        return ResponseEntity.ok(book);
    }

    @PatchMapping("/{bookId}/restore")
    public ResponseEntity<AdminBookResponse> restoreBook(@PathVariable Long bookId) {
        AdminBookResponse book = adminBookService.restoreBook(bookId);
        return ResponseEntity.ok(book);
    }

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "false") boolean hard) {
        adminBookService.deleteBook(bookId, hard);
        return ResponseEntity.noContent().build();
    }

    // SKU Management
    
    @PostMapping("/{bookId}/skus")
    public ResponseEntity<AdminBookResponse> addSku(
            @PathVariable Long bookId,
            @Valid @RequestBody SkuCreateRequest request) {
        AdminBookResponse book = adminBookService.addSku(bookId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(book);
    }

    @PutMapping("/{bookId}/skus/{skuId}")
    public ResponseEntity<AdminBookResponse> updateSku(
            @PathVariable Long bookId,
            @PathVariable Long skuId,
            @Valid @RequestBody SkuUpdateRequest request) {
        AdminBookResponse book = adminBookService.updateSku(bookId, skuId, request);
        return ResponseEntity.ok(book);
    }

    @PatchMapping("/{bookId}/skus/{skuId}/set-default")
    public ResponseEntity<AdminBookResponse> setDefaultSku(
            @PathVariable Long bookId,
            @PathVariable Long skuId) {
        AdminBookResponse book = adminBookService.setDefaultSku(bookId, skuId);
        return ResponseEntity.ok(book);
    }

    @DeleteMapping("/{bookId}/skus/{skuId}")
    public ResponseEntity<AdminBookResponse> deleteSku(
            @PathVariable Long bookId,
            @PathVariable Long skuId) {
        AdminBookResponse book = adminBookService.deleteSku(bookId, skuId);
        return ResponseEntity.ok(book);
    }
}
