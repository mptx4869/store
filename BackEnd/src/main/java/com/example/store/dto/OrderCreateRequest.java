package com.example.store.dto;

import jakarta.validation.constraints.Size;

public record OrderCreateRequest(
    @Size(max = 500, message = "Shipping address must be at most 500 characters")
    String shippingAddress,
    
    @Size(max = 20, message = "Shipping phone must be at most 20 characters")
    String shippingPhone,
    
    @Size(max = 500, message = "Billing address must be at most 500 characters")
    String billingAddress,
    
    @Size(max = 20, message = "Billing phone must be at most 20 characters")
    String billingPhone,
    
    @Size(max = 10, message = "Currency must be at most 10 characters")
    String currency
) {}
