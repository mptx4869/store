package com.example.store.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderItemResponse {
    Long bookId;
    String title;
    String sku;
    int quantity;
    BigDecimal unitPrice;
    BigDecimal lineTotal;
}
