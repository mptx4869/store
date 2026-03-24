package com.example.store.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    // Admin user management methods
    Integer countByUserId(Long userId);

    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT o FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND o.placedAt >= :startDate AND o.placedAt <= :endDate")
    List<Order> findCompletedOrdersBetweenDates(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);
}
