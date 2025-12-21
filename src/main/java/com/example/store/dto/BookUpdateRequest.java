package com.example.store.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookUpdateRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Size(max = 255, message = "Subtitle must not exceed 255 characters")
    private String subtitle;
    
    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    private String description;
    
    @Size(max = 50, message = "Language code must not exceed 50 characters")
    private String language;
    
    @Positive(message = "Pages must be positive")
    private Integer pages;
    
    private Long publisherId;
    
    private LocalDate publishedDate;
    
    @NotNull(message = "Base price is required")
    @Positive(message = "Base price must be positive")
    private BigDecimal basePrice;
}
