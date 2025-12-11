package com.example.store.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class BookResponse {
    Long id;
    String title;
    String subtitle;
    String description;
    String language;
    Integer pages;
    LocalDate publishedDate;
    BigDecimal price;
    String sku;
}
