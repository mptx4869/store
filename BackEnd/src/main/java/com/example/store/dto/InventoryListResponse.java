package com.example.store.dto;

import java.time.LocalDateTime;

import lombok.Builder;

@Builder
public record InventoryListResponse(
    Long skuId,
    String sku,
    Long bookId,
    String bookTitle,
    String format,
    Integer totalStock,
    Integer reservedStock,
    Integer availableStock,
    String status,
    LocalDateTime lastUpdated
) {
}
