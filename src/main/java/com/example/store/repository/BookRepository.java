package com.example.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.model.Book;

public interface BookRepository extends JpaRepository<Book, Long> {

    boolean existsByTitle(String title);
}
