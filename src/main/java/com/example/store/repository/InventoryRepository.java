package com.example.store.repository;

import java.util.Optional;

import jakarta.persistence.LockModeType;
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
}
