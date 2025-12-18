package com.example.store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.store.model.Order;
import com.example.store.model.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

    @EntityGraph(attributePaths = {"orderItems", "orderItems.book", "orderItems.productSku"})
    Optional<Order> findByUserAndStatus(User user, String status);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.book", "orderItems.productSku"})
    List<Order> findByUserOrderByCreatedAtDesc(User user);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.book", "orderItems.productSku"})
    Page<Order> findByStatus(String status, Pageable pageable);

    @EntityGraph(attributePaths = {"orderItems", "orderItems.book", "orderItems.productSku"})
    Page<Order> findAll(Pageable pageable);
}
