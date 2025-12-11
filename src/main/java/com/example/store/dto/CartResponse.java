package com.example.store.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class CartResponse {
    Long cartId;
    String status;
    BigDecimal totalAmount;
    Integer totalItems;
    List<CartItemResponse> items;
}
