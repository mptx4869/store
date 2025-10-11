package com.example.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

@Entity
@Table(name = "books")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Book {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "book_id")
    private Long bookId;

    @Column(nullable = false)
    private String title;

    private String author;
    private String publisher;

    @Column(name = "published_date")
    private LocalDate publishedDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    private BigDecimal price;
    private Integer stock;
    private String isbn;

    @Column(name = "cover_image")
    private String coverImage;

    // Relationships
    @OneToMany(mappedBy = "book", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<BookCategory> bookCategories;
}
