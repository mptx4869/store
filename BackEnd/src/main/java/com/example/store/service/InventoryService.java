package com.example.store.service;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.InventoryListResponse;
import com.example.store.dto.InventoryUpdateRequest;
import com.example.store.dto.StockResponse;
import com.example.store.exception.ConflictException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Book;
import com.example.store.model.Inventory;
import com.example.store.model.ProductSku;
import com.example.store.repository.BookRepository;
import com.example.store.repository.InventoryRepository;
import com.example.store.repository.ProductSkuRepository;

@Service
public class InventoryService {
    
    private static final int LOW_STOCK_THRESHOLD = 10;
    
    private final InventoryRepository inventoryRepository;
    private final BookRepository bookRepository;
    private final ProductSkuRepository productSkuRepository;
    
    public InventoryService(
        InventoryRepository inventoryRepository,
        BookRepository bookRepository,
        ProductSkuRepository productSkuRepository
    ) {
        this.inventoryRepository = inventoryRepository;
        this.bookRepository = bookRepository;
        this.productSkuRepository = productSkuRepository;
    }
    
    /**
     * Get stock information for a book (using default SKU)
     */
    @Transactional(readOnly = true)
    public StockResponse getStockByBookId(Long bookId) {
        Book book = bookRepository.findById(bookId)
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        
        // Get default SKU
        ProductSku productSku = resolveDefaultSku(book);
        
        return getStockBySkuId(productSku.getId());
    }
    
    /**
     * Get stock information for a specific SKU
     */
    @Transactional(readOnly = true)
    public StockResponse getStockBySkuId(Long skuId) {
        ProductSku productSku = productSkuRepository.findById(skuId)
            .orElseThrow(() -> new ResourceNotFoundException("Product SKU not found"));
        
        Inventory inventory = inventoryRepository.findByProductSkuId(skuId)
            .orElse(null);
        
        if (inventory == null) {
            // No inventory record = out of stock
            return StockResponse.builder()
                .bookId(productSku.getBook().getId())
                .skuId(skuId)
                .sku(productSku.getSku())
                .totalStock(0)
                .reservedStock(0)
                .availableStock(0)
                .inStock(false)
                .status("OUT_OF_STOCK")
                .build();
        }
        
        int totalStock = inventory.getStock() != null ? inventory.getStock() : 0;
        int reservedStock = inventory.getReserved() != null ? inventory.getReserved() : 0;
        int availableStock = totalStock - reservedStock;
        boolean inStock = availableStock > 0;
        
        String status;
        if (availableStock == 0) {
            status = "OUT_OF_STOCK";
        } else if (availableStock <= LOW_STOCK_THRESHOLD) {
            status = "LOW_STOCK";
        } else {
            status = "IN_STOCK";
        }
        
        return StockResponse.builder()
            .bookId(productSku.getBook().getId())
            .skuId(skuId)
            .sku(productSku.getSku())
            .totalStock(totalStock)
            .reservedStock(reservedStock)
            .availableStock(availableStock)
            .inStock(inStock)
            .status(status)
            .build();
    }
    
    /**
     * Reserve stock for an order (pessimistic locking prevents race conditions)
     * This increases the reserved count and validates sufficient stock is available
     */
    @Transactional
    public void reserveStock(Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        // Lock the inventory row to prevent concurrent modifications
        Inventory inventory = inventoryRepository.findByProductSkuIdForUpdate(skuId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + skuId));
        
        int currentStock = inventory.getStock() != null ? inventory.getStock() : 0;
        int currentReserved = inventory.getReserved() != null ? inventory.getReserved() : 0;
        int availableStock = currentStock - currentReserved;
        
        if (availableStock < quantity) {
            throw new ConflictException(
                String.format("Insufficient stock. Available: %d, Requested: %d", 
                    availableStock, quantity)
            );
        }
        
        // Update reserved count
        inventory.setReserved(currentReserved + quantity);
        inventoryRepository.save(inventory);
    }
    
    /**
     * Release reserved stock (when order is cancelled)
     * This decreases the reserved count
     */
    @Transactional
    public void releaseStock(Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        // Lock the inventory row
        Inventory inventory = inventoryRepository.findByProductSkuIdForUpdate(skuId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + skuId));
        
        int currentReserved = inventory.getReserved() != null ? inventory.getReserved() : 0;
        
        if (currentReserved < quantity) {
            throw new ConflictException(
                String.format("Cannot release more than reserved. Reserved: %d, Requested: %d", 
                    currentReserved, quantity)
            );
        }
        
        // Decrease reserved count
        inventory.setReserved(currentReserved - quantity);
        inventoryRepository.save(inventory);
    }
    
    /**
     * Fulfill stock (when order is delivered)
     * This decreases both stock and reserved counts
     */
    @Transactional
    public void fulfillStock(Long skuId, int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        
        // Lock the inventory row
        Inventory inventory = inventoryRepository.findByProductSkuIdForUpdate(skuId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + skuId));
        
        int currentStock = inventory.getStock() != null ? inventory.getStock() : 0;
        int currentReserved = inventory.getReserved() != null ? inventory.getReserved() : 0;
        
        if (currentStock < quantity) {
            throw new ConflictException(
                String.format("Cannot fulfill more than stock. Stock: %d, Requested: %d", 
                    currentStock, quantity)
            );
        }
        
        if (currentReserved < quantity) {
            throw new ConflictException(
                String.format("Cannot fulfill more than reserved. Reserved: %d, Requested: %d", 
                    currentReserved, quantity)
            );
        }
        
        // Decrease both stock and reserved
        inventory.setStock(currentStock - quantity);
        inventory.setReserved(currentReserved - quantity);
        inventoryRepository.save(inventory);
    }
    
    private ProductSku resolveDefaultSku(Book book) {
        if (book.getDefaultSkuId() != null) {
            return productSkuRepository.findById(book.getDefaultSkuId())
                .orElseThrow(() -> new ResourceNotFoundException("Product SKU not found for book"));
        }
        return productSkuRepository.findFirstByBookId(book.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Product SKU not found for book"));
    }
    
    // ==================== ADMIN METHODS ====================
    
    /**
     * Get all inventory with pagination (Admin only)
     */
    @Transactional(readOnly = true)
    public Page<InventoryListResponse> getAllInventory(Pageable pageable) {
        Page<Inventory> inventoryPage = inventoryRepository.findAll(pageable);
        return inventoryPage.map(this::mapToListResponse);
    }
    
    /**
     * Get low stock inventory items (Admin only)
     */
    @Transactional(readOnly = true)
    public List<InventoryListResponse> getLowStockInventory() {
        List<Inventory> allInventory = inventoryRepository.findAll();
        
        return allInventory.stream()
            .filter(inventory -> {
                int stock = inventory.getStock() != null ? inventory.getStock() : 0;
                int reserved = inventory.getReserved() != null ? inventory.getReserved() : 0;
                int available = stock - reserved;
                return available <= LOW_STOCK_THRESHOLD;
            })
            .map(this::mapToListResponse)
            .toList();
    }
    
    /**
     * Update inventory stock (Admin only)
     * Supports two actions:
     * - ADD: Adds to existing stock (e.g., receiving new shipment)
     * - SET: Sets absolute stock value (e.g., inventory correction)
     */
    @Transactional
    public InventoryListResponse updateInventoryStock(Long skuId, InventoryUpdateRequest request) {
        // Lock the inventory row
        Inventory inventory = inventoryRepository.findByProductSkuIdForUpdate(skuId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found for SKU: " + skuId));
        
        if (request.stock() != null) {
            String action = request.action() != null ? request.action() : "SET";
            
            if ("ADD".equals(action)) {
                // Add to existing stock (receiving shipment)
                int currentStock = inventory.getStock() != null ? inventory.getStock() : 0;
                inventory.setStock(currentStock + request.stock());
            } else {
                // Set absolute stock value (inventory correction)
                inventory.setStock(request.stock());
            }
            
            // Validate: stock must be >= reserved
            int newStock = inventory.getStock();
            int reserved = inventory.getReserved() != null ? inventory.getReserved() : 0;
            if (newStock < reserved) {
                throw new ConflictException(
                    String.format("Cannot set stock to %d. Reserved stock is %d", newStock, reserved)
                );
            }
        }
        
        if (request.reserved() != null) {
            inventory.setReserved(request.reserved());
        }
        
        Inventory saved = inventoryRepository.save(inventory);
        return mapToListResponse(saved);
    }
    
    private InventoryListResponse mapToListResponse(Inventory inventory) {
        ProductSku productSku = inventory.getProductSku();
        Book book = productSku != null ? productSku.getBook() : null;
        
        int totalStock = inventory.getStock() != null ? inventory.getStock() : 0;
        int reservedStock = inventory.getReserved() != null ? inventory.getReserved() : 0;
        int availableStock = totalStock - reservedStock;
        
        String status;
        if (availableStock == 0) {
            status = "OUT_OF_STOCK";
        } else if (availableStock <= LOW_STOCK_THRESHOLD) {
            status = "LOW_STOCK";
        } else {
            status = "IN_STOCK";
        }
        
        return InventoryListResponse.builder()
            .skuId(inventory.getId())
            .sku(productSku != null ? productSku.getSku() : null)
            .bookId(book != null ? book.getId() : null)
            .bookTitle(book != null ? book.getTitle() : null)
            .format(productSku != null ? productSku.getFormat() : null)
            .totalStock(totalStock)
            .reservedStock(reservedStock)
            .availableStock(availableStock)
            .status(status)
            .lastUpdated(inventory.getLastUpdated())
            .build();
    }
}
