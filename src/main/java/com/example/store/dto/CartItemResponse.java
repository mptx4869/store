package com.example.store.dto;

import java.math.BigDecimal;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CartItemResponse {
    Long itemId;
    Long bookId;
    String title;
    String sku;
    int quantity;
    BigDecimal price;           // Current/latest price
    BigDecimal originalPrice;   // Price when added to cart
    Boolean priceChanged;       // Flag indicating if price changed
    BigDecimal priceDiff;       // Difference (negative = cheaper, positive = more expensive)
}
