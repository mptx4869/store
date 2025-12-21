package com.example.store.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Order {

    // Order status constants
    public static final String STATUS_PLACED = "PLACED";
    public static final String STATUS_CONFIRMED = "CONFIRMED";
    public static final String STATUS_PROCESSING = "PROCESSING";
    public static final String STATUS_SHIPPED = "SHIPPED";
    public static final String STATUS_DELIVERED = "DELIVERED";
    public static final String STATUS_CANCELLED = "CANCELLED";
    public static final String STATUS_RETURNED = "RETURNED";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id")
    private ShoppingCart cart;

    @Column(name = "shipping_address", length = 500)
    private String shippingAddress;

    @Column(name = "shipping_phone", length = 20)
    private String shippingPhone;

    @Column(name = "billing_address", length = 500)
    private String billingAddress;

    @Column(name = "billing_phone", length = 20)
    private String billingPhone;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

    @Column(length = 10)
    private String currency;

    @Column(length = 30, nullable = false)
    private String status;

    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "placed_at")
    private LocalDateTime placedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    /**
     * Check if order can be cancelled
     */
    public boolean isCancellable() {
        return STATUS_PLACED.equals(status) || STATUS_CONFIRMED.equals(status);
    }

    /**
     * Check if order is in a final state
     */
    public boolean isFinalState() {
        return STATUS_DELIVERED.equals(status) || 
               STATUS_CANCELLED.equals(status) || 
               STATUS_RETURNED.equals(status);
    }

    /**
     * Check if order is active (not cancelled/returned)
     */
    public boolean isActive() {
        return !STATUS_CANCELLED.equals(status) && !STATUS_RETURNED.equals(status);
    }
}
