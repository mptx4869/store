package com.example.store.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.model.ShoppingCart;

public interface ShoppingCartRepository extends JpaRepository<ShoppingCart, Long> {

    @EntityGraph(attributePaths = {"items", "items.productSku", "items.productSku.book"})
    Optional<ShoppingCart> findByUserIdAndStatus(Long userId, String status);
}
