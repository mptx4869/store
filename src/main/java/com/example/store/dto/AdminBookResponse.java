package com.example.store.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminBookResponse {
    
    private Long id;
    private String title;
    private String subtitle;
    private String description;
    private String language;
    private Integer pages;
    private String publisherName;
    private Long publisherId;
    private LocalDate publishedDate;
    private BigDecimal basePrice;
    private Long defaultSkuId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    
    private List<SkuInfo> skus;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkuInfo {
        private Long id;
        private String sku;
        private String format;
        private BigDecimal price;
        private Integer stock;
        private Integer reserved;
        private Integer available;
        private Boolean isDefault;
    }
}
