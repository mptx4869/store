package com.example.store.dto;

import java.math.BigDecimal;

public record RevenueResponse(String period, BigDecimal totalRevenue, int orderCount) {
}
