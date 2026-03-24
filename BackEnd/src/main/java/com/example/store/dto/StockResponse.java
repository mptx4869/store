package com.example.store.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class StockResponse {
    Long bookId;
    Long skuId;
    String sku;
    Integer totalStock;      // Total stock in inventory
    Integer reservedStock;   // Reserved for pending orders
    Integer availableStock;  // Available = total - reserved
    Boolean inStock;         // True if available > 0
    String status;           // LOW_STOCK, IN_STOCK, OUT_OF_STOCK
}
