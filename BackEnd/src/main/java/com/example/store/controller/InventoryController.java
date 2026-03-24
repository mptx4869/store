package com.example.store.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.StockResponse;
import com.example.store.service.InventoryService;

@RestController
@RequestMapping("/inventory")
public class InventoryController {
    
    private final InventoryService inventoryService;
    
    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    /**
     * Get stock information for a book (public endpoint)
     * GET /inventory/books/{bookId}
     */
    @GetMapping("/books/{bookId}")
    public ResponseEntity<StockResponse> getStockByBook(@PathVariable Long bookId) {
        StockResponse stock = inventoryService.getStockByBookId(bookId);
        return ResponseEntity.ok(stock);
    }
    
    /**
     * Get stock information for a specific SKU
     * GET /inventory/skus/{skuId}
     */
    @GetMapping("/skus/{skuId}")
    public ResponseEntity<StockResponse> getStockBySku(@PathVariable Long skuId) {
        StockResponse stock = inventoryService.getStockBySkuId(skuId);
        return ResponseEntity.ok(stock);
    }
}
