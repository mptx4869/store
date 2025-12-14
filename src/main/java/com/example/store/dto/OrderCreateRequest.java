package com.example.store.dto;

import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
    Long shippingAddressId,
    Long billingAddressId,
    @Size(max = 10, message = "Currency must be at most 10 characters")
    String currency
) {}
