package com.example.store.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CartItemRequest(
    @NotNull(message = "SKU ID is required") 
    Long skuId,
    @Min(value = 1, message = "Quantity must be at least 1") 
    int quantity
) {}
