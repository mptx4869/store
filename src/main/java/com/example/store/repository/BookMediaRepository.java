package com.example.store.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.model.BookMedia;

public interface BookMediaRepository extends JpaRepository<BookMedia, Long> {

    List<BookMedia> findByBookId(Long bookId);
}
