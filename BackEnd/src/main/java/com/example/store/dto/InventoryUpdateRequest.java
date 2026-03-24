package com.example.store.dto;

public record InventoryUpdateRequest(
    Integer stock,
    Integer reserved,
    String action  // "ADD" or "SET"
) {
    public InventoryUpdateRequest {
        if (stock != null && stock < 0) {
            throw new IllegalArgumentException("Stock cannot be negative");
        }
        if (reserved != null && reserved < 0) {
            throw new IllegalArgumentException("Reserved cannot be negative");
        }
        if (action != null && !action.equals("ADD") && !action.equals("SET")) {
            throw new IllegalArgumentException("Action must be 'ADD' or 'SET'");
        }
    }
}
