package com.example.store.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.InventoryListResponse;
import com.example.store.dto.InventoryUpdateRequest;
import com.example.store.service.InventoryService;

@RestController
@RequestMapping("/admin/inventory")
@PreAuthorize("hasRole('ADMIN')")
public class AdminInventoryController {
    
    private final InventoryService inventoryService;
    
    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }
    
    /**
     * Get all inventory with pagination
     * GET /admin/inventory?page=0&size=20
     */
    @GetMapping
    public ResponseEntity<Page<InventoryListResponse>> getAllInventory(
        @PageableDefault(size = 20, sort = "lastUpdated", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<InventoryListResponse> inventory = inventoryService.getAllInventory(pageable);
        return ResponseEntity.ok(inventory);
    }
    
    /**
     * Get low stock items (stock < threshold)
     * GET /admin/inventory/low-stock
     */
    @GetMapping("/low-stock")
    public ResponseEntity<List<InventoryListResponse>> getLowStockInventory() {
        List<InventoryListResponse> lowStock = inventoryService.getLowStockInventory();
        return ResponseEntity.ok(lowStock);
    }
    
    /**
     * Update inventory stock
     * PUT /admin/inventory/{skuId}
     * Body: { "stock": 100, "action": "ADD" } or { "stock": 50, "action": "SET" }
     */
    @PutMapping("/{skuId}")
    public ResponseEntity<InventoryListResponse> updateInventory(
        @PathVariable Long skuId,
        @RequestBody InventoryUpdateRequest request
    ) {
        InventoryListResponse updated = inventoryService.updateInventoryStock(skuId, request);
        return ResponseEntity.ok(updated);
    }
}
