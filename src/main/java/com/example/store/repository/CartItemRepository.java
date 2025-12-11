package com.example.store.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.model.CartItem;
import com.example.store.model.ProductSku;
import com.example.store.model.ShoppingCart;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartAndProductSku(ShoppingCart cart, ProductSku productSku);
}
