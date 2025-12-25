package com.example.store.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BookResponse {
    Long id;
    String title;
    String subtitle;
    String description;
    String language;
    Integer pages;
    LocalDate publishedDate;
    BigDecimal price;
    String sku;
    String imageUrl;
    List<SkuInfo> skus;
    
    @Value
    @Builder
    public static class SkuInfo {
        Long id;
        String sku;
        String format;
        BigDecimal price;
        Boolean inStock;
        Integer availableStock;
        Boolean isDefault;
        
        // Physical dimensions for shipping
        Integer weightGrams;
        Integer lengthMm;
        Integer widthMm;
        Integer heightMm;
    }
}
