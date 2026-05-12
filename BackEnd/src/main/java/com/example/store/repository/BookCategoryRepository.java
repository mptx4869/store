package com.example.store.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.model.BookCategory;
import com.example.store.model.BookCategoryId;

public interface BookCategoryRepository extends JpaRepository<BookCategory, BookCategoryId> {

	@Query("""
		SELECT bc.book.id AS bookId,
			   bc.category.id AS categoryId,
			   bc.category.name AS categoryName
		FROM BookCategory bc
		WHERE bc.book.id IN :bookIds
		""")
	List<CategoryRow> findCategoryRowsByBookIdIn(@Param("bookIds") List<Long> bookIds);

	@Query("""
		SELECT bc.book.id AS bookId,
		       bc.book.title AS bookTitle
		FROM BookCategory bc
		WHERE bc.category.id = :categoryId
		""")
	List<BookRow> findBookRowsByCategoryId(@Param("categoryId") Long categoryId);

	@Modifying
	@Query("DELETE FROM BookCategory bc WHERE bc.book.id = :bookId")
	int deleteByBookId(@Param("bookId") Long bookId);

	interface CategoryRow {
		Long getBookId();

		Long getCategoryId();

		String getCategoryName();
	}

	interface BookRow {
		Long getBookId();

		String getBookTitle();
	}
}
