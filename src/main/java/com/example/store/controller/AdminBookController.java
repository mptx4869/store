package com.example.store.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.AdminBookResponse;
import com.example.store.dto.BookCreateRequest;
import com.example.store.dto.BookUpdateRequest;
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
            @RequestParam(required = false) Boolean includeDeleted) {
        
        Sort sort = sortDirection.equalsIgnoreCase("DESC") 
                ? Sort.by(sortBy).descending() 
                : Sort.by(sortBy).ascending();
        
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<AdminBookResponse> books = adminBookService.getAllBooks(pageable, includeDeleted);
        
        return ResponseEntity.ok(books);
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

    @DeleteMapping("/{bookId}")
    public ResponseEntity<Void> deleteBook(
            @PathVariable Long bookId,
            @RequestParam(defaultValue = "false") boolean hard) {
        adminBookService.deleteBook(bookId, hard);
        return ResponseEntity.noContent().build();
    }
}
