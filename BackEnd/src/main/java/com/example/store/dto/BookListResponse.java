package com.example.store.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BookListResponse {
    Long id;
    String title;
    String imageUrl;
    BigDecimal basePrice;
    LocalDateTime createdAt;
    List<CategoryInfo> categories;

    @Value
    @Builder
    public static class CategoryInfo {
        Long id;
        String name;
    }
}
