package com.example.store.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class OrderResponse {
    Long orderId;
    String status;
    String currency;
    BigDecimal totalAmount;
    LocalDateTime placedAt;
    Long cartId;
    Long shippingAddressId;
    Long billingAddressId;
    List<OrderItemResponse> items;
}
