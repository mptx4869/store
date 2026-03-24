package com.example.store.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.model.Book;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.ProductSku;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    Optional<OrderItem> findByOrderAndProductSku(Order order, ProductSku productSku);

    Optional<OrderItem> findByOrderAndBook(Order order, Book book);
}
