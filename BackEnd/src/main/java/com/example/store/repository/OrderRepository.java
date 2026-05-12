package com.example.store.repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // User history - uses userId directly to avoid loading full User object
    @EntityGraph(attributePaths = {"orderItems", "orderItems.book", "orderItems.productSku"})
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT o FROM Order o WHERE o.status IN ('PAID', 'SHIPPED', 'DELIVERED') AND o.placedAt >= :startDate AND o.placedAt <= :endDate")
    List<Order> findCompletedOrdersBetweenDates(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    /**
     * Single aggregate query replacing load-all-orders in getUserById.
     * COUNT(*) always returns 1 row (even when user has no orders), so the
     * return type is non-Optional.
     * FILTER (WHERE ...) is PostgreSQL syntax for conditional aggregation.
     */
    @Query(value = """
            SELECT COUNT(*)                                                    AS totalOrders,
                   COUNT(*) FILTER (WHERE status = 'DELIVERED')               AS completedOrders,
                   COUNT(*) FILTER (WHERE status = 'CANCELLED')               AS cancelledOrders,
                   COALESCE(SUM(total_amount) FILTER (WHERE status = 'DELIVERED'), 0) AS totalSpent
            FROM orders
            WHERE user_id = :userId
            """, nativeQuery = true)
    UserOrderStats findUserOrderStatsByUserId(@Param("userId") Long userId);

    /**
     * Returns the N most recent orders for a user with only the fields needed
     * by UserDetailResponse — avoids loading full Order entities and their items.
     * Pass PageRequest.of(0, 5) from the caller to get the 5 most recent.
     */
    @Query("""
            SELECT o.id          AS id,
                   o.status      AS status,
                   o.totalAmount AS totalAmount,
                   o.createdAt   AS createdAt
            FROM Order o
            WHERE o.user.id = :userId
            ORDER BY o.createdAt DESC
            """)
    List<RecentOrderSummary> findRecentOrdersByUserId(@Param("userId") Long userId, Pageable pageable);

    interface UserOrderStats {
        Long getTotalOrders();
        Long getCompletedOrders();
        Long getCancelledOrders();
        BigDecimal getTotalSpent();
    }

    interface RecentOrderSummary {
        Long getId();
        String getStatus();
        BigDecimal getTotalAmount();
        LocalDateTime getCreatedAt();
    }
}
