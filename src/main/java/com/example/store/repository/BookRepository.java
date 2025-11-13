package com.example.store.repository;

import com.example.store.model.Book;

import org.springframework.data.repository.Repository;

public interface BookRepository extends Repository <Book, Long> {
    
    Book save(Book book);
    Book findById(Long bookId);
    void deleteById(Long bookId);
    
}
