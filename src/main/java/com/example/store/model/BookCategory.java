package com.example.store.model;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@IdClass(BookCategoryId.class)
@Table(name = "book_categories")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookCategory {

    @Id
    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Id
    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    private Integer priority;
}
