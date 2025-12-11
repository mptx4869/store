package com.example.store.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.model.Inventory;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {
}
