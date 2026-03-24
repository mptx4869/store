package com.example.store.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SkuCreateRequest {
    
    @NotBlank(message = "SKU code is required")
    @Size(max = 100, message = "SKU code must not exceed 100 characters")
    private String sku;
    
    @NotBlank(message = "Format is required")
    @Size(max = 100, message = "Format must not exceed 100 characters")
    private String format;
    
    @Positive(message = "Price override must be positive if provided")
    private BigDecimal priceOverride;
    
    @Positive(message = "Initial stock must be positive if provided")
    private Integer initialStock;
    
    // Physical dimensions
    private Integer weightGrams;
    private Integer lengthMm;
    private Integer widthMm;
    private Integer heightMm;
    
    private Boolean isDefault;
}
