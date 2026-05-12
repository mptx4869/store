package com.example.store.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.example.store.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
    
    Optional<Inventory> findByProductSkuId(Long skuId);
    
    /**
     * Find inventory with pessimistic write lock to prevent race conditions
     * This will lock the row until the transaction completes
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.productSku.id = :skuId")
    Optional<Inventory> findByProductSkuIdForUpdate(@Param("skuId") Long skuId);

    @Query("""
        SELECT COUNT(i) FROM Inventory i
        WHERE (COALESCE(i.stock, 0) - COALESCE(i.reserved, 0)) <= :threshold
        """)
    long countLowStock(@Param("threshold") int threshold);

    @Query("""
        SELECT i.id AS skuId,
               ps.sku AS sku,
               b.id AS bookId,
               b.title AS bookTitle,
               ps.format AS format,
               COALESCE(i.stock, 0) AS totalStock,
               COALESCE(i.reserved, 0) AS reservedStock,
               (COALESCE(i.stock, 0) - COALESCE(i.reserved, 0)) AS availableStock,
               i.lastUpdated AS lastUpdated
        FROM Inventory i
        JOIN i.productSku ps
        JOIN ps.book b
        WHERE (COALESCE(i.stock, 0) - COALESCE(i.reserved, 0)) <= :threshold
        """)
    Page<InventoryListRow> findLowStockRows(@Param("threshold") int threshold, Pageable pageable);

    interface InventoryListRow {
        Long getSkuId();

        String getSku();

        Long getBookId();

        String getBookTitle();

        String getFormat();

        Integer getTotalStock();

        Integer getReservedStock();

        Integer getAvailableStock();

        LocalDateTime getLastUpdated();
    }
}
