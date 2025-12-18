package com.example.store.dto;

import jakarta.validation.constraints.Min;

public record CartItemUpdateRequest(
    @Min(value = 1, message = "Quantity must be at least 1") 
    int quantity
) {}
