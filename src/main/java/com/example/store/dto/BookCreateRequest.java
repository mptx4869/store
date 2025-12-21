package com.example.store.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
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
public class BookCreateRequest {
    
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
    
    @NotEmpty(message = "At least one SKU is required")
    @Valid
    private List<SkuCreateRequest> skus;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuCreateRequest {
        
        @NotBlank(message = "SKU code is required")
        @Size(max = 100, message = "SKU code must not exceed 100 characters")
        private String sku;
        
        @NotBlank(message = "Format is required")
        @Size(max = 100, message = "Format must not exceed 100 characters")
        private String format;
        
        @Positive(message = "Price override must be positive if provided")
        private BigDecimal priceOverride;
        
        @Positive(message = "Initial stock must be positive if provided")
        private Integer initialStock;
        
        // Physical dimensions
        private Integer weightGrams;
        private Integer lengthMm;
        private Integer widthMm;
        private Integer heightMm;
        
        private Boolean isDefault;
    }
}
