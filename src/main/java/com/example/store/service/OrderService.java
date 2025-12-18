package com.example.store.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.OrderCreateRequest;
import com.example.store.dto.OrderItemResponse;
import com.example.store.dto.OrderResponse;
import com.example.store.exception.ConflictException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.CartItem;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.ProductSku;
import com.example.store.model.ShoppingCart;
import com.example.store.model.User;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ShoppingCartRepository;
import com.example.store.repository.UserRepository;

@Service
public class OrderService {

    private static final String CART_STATUS_ACTIVE = "ACTIVE";
    private static final String CART_STATUS_COMPLETED = "COMPLETED";
    private static final String DEFAULT_CURRENCY = "USD";

    private final OrderRepository orderRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UserRepository userRepository;
    private final InventoryService inventoryService;

    public OrderService(
        OrderRepository orderRepository,
        ShoppingCartRepository shoppingCartRepository,
        UserRepository userRepository,
        InventoryService inventoryService
    ) {
        this.orderRepository = orderRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.userRepository = userRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public OrderResponse createOrder(String username, OrderCreateRequest request) {
        User user = fetchUser(username);
        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), CART_STATUS_ACTIVE)
            .orElseThrow(() -> new ConflictException("No active cart available for checkout."));

        Set<CartItem> cartItems = new HashSet<>(cart.getItems());
        if (cartItems.isEmpty()) {
            throw new ConflictException("Cannot create an order from an empty cart.");
        }

        // Reserve stock with pessimistic locking (prevents race conditions)
        for (CartItem cartItem : cartItems) {
            ProductSku productSku = cartItem.getProductSku();
            if (productSku == null || productSku.getBook() == null) {
                throw new ConflictException("Cart item is missing product details.");
            }
            // This will lock inventory rows and validate stock in one atomic operation
            inventoryService.reserveStock(productSku.getId(), cartItem.getQuantity());
        }

        BigDecimal totalAmount = cartItems.stream()
            .map(this::calculateLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
            .user(user)
            .cart(cart)
            .status(Order.STATUS_PLACED)
            .currency(resolveCurrency(request))
            .totalAmount(totalAmount)
            .shippingAddress(request != null ? request.shippingAddress() : null)
            .shippingPhone(request != null ? request.shippingPhone() : null)
            .billingAddress(request != null ? request.billingAddress() : null)
            .billingPhone(request != null ? request.billingPhone() : null)
            .placedAt(LocalDateTime.now())
            .build();

        cartItems.forEach(cartItem -> {
            ProductSku productSku = cartItem.getProductSku();
            OrderItem orderItem = OrderItem.builder()
                .order(order)
                .book(productSku.getBook())
                .productSku(productSku)
                .quantity(cartItem.getQuantity())
                .unitPrice(cartItem.getUnitPrice())
                .build();
            order.getOrderItems().add(orderItem);
        });

        Order savedOrder = orderRepository.save(order);

        cart.getItems().clear();
        cart.setStatus(CART_STATUS_COMPLETED);
        cart.setSubtotal(BigDecimal.ZERO);
        cart.setTotalItems(0);
        shoppingCartRepository.save(cart);

        return mapToResponse(savedOrder);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getOrderHistory(String username) {
        User user = fetchUser(username);
        return orderRepository.findByUserOrderByCreatedAtDesc(user).stream()
            .map(this::mapToResponse)
            .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(String username, Long orderId) {
        User user = fetchUser(username);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        // Authorization check - user can only view their own orders
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found");
        }
        
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse cancelOrder(String username, Long orderId) {
        User user = fetchUser(username);
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        // Authorization check
        if (!order.getUser().getId().equals(user.getId())) {
            throw new ResourceNotFoundException("Order not found");
        }
        
        // Validation check - can only cancel orders in PLACED or CONFIRMED status
        if (!order.isCancellable()) {
            throw new ConflictException(
                "Order cannot be cancelled. Current status: " + order.getStatus()
            );
        }
        
        // Release reserved stock back to available inventory
        for (OrderItem item : order.getOrderItems()) {
            ProductSku productSku = item.getProductSku();
            if (productSku != null) {
                inventoryService.releaseStock(productSku.getId(), item.getQuantity());
            }
        }
        
        order.setStatus(Order.STATUS_CANCELLED);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        
        return mapToResponse(savedOrder);
    }

    private User fetchUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private String resolveCurrency(OrderCreateRequest request) {
        if (request != null && request.currency() != null && !request.currency().isBlank()) {
            return request.currency();
        }
        return DEFAULT_CURRENCY;
    }

    private BigDecimal calculateLineTotal(CartItem cartItem) {
        BigDecimal unitPrice = cartItem.getUnitPrice() != null ? cartItem.getUnitPrice() : BigDecimal.ZERO;
        int quantity = cartItem.getQuantity() != null ? cartItem.getQuantity() : 0;
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> items = order.getOrderItems().stream()
            .sorted(Comparator.comparing(item -> {
                ProductSku sku = item.getProductSku();
                return sku != null ? sku.getSku() : "";
            }))
            .map(this::mapToItemResponse)
            .toList();

        return OrderResponse.builder()
            .orderId(order.getId())
            .status(order.getStatus())
            .currency(order.getCurrency())
            .totalAmount(order.getTotalAmount())
            .placedAt(order.getPlacedAt())
            .cartId(order.getCart() != null ? order.getCart().getId() : null)
            .shippingAddress(order.getShippingAddress())
            .shippingPhone(order.getShippingPhone())
            .billingAddress(order.getBillingAddress())
            .billingPhone(order.getBillingPhone())
            .items(items)
            .build();
    }

    private OrderItemResponse mapToItemResponse(OrderItem orderItem) {
        BigDecimal unitPrice = orderItem.getUnitPrice() != null ? orderItem.getUnitPrice() : BigDecimal.ZERO;
        int quantity = orderItem.getQuantity() != null ? orderItem.getQuantity() : 0;
        BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));

        return OrderItemResponse.builder()
            .bookId(orderItem.getBook() != null ? orderItem.getBook().getId() : null)
            .title(orderItem.getBook() != null ? orderItem.getBook().getTitle() : null)
            .sku(orderItem.getProductSku() != null ? orderItem.getProductSku().getSku() : null)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .lineTotal(lineTotal)
            .build();
    }

    // Admin methods
    @Transactional(readOnly = true)
    public Page<OrderResponse> getAllOrders(String status, Pageable pageable) {
        Page<Order> orders;
        if (status != null && !status.isEmpty()) {
            orders = orderRepository.findByStatus(status, pageable);
        } else {
            orders = orderRepository.findAll(pageable);
        }
        return orders.map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByIdAdmin(Long orderId) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return mapToResponse(order);
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, String newStatus) {
        Order order = orderRepository.findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        
        // Validate status
        if (newStatus == null || newStatus.isEmpty()) {
            throw new ConflictException("Status cannot be empty");
        }
        
        // Validate status flow
        validateStatusTransition(order.getStatus(), newStatus);
        
        // Handle inventory changes based on status transition
        if (Order.STATUS_DELIVERED.equals(newStatus)) {
            // Fulfill stock: decrease both stock and reserved
            for (OrderItem item : order.getOrderItems()) {
                ProductSku productSku = item.getProductSku();
                if (productSku != null) {
                    inventoryService.fulfillStock(productSku.getId(), item.getQuantity());
                }
            }
        } else if (Order.STATUS_CANCELLED.equals(newStatus)) {
            // Release reserved stock back to available inventory
            for (OrderItem item : order.getOrderItems()) {
                ProductSku productSku = item.getProductSku();
                if (productSku != null) {
                    inventoryService.releaseStock(productSku.getId(), item.getQuantity());
                }
            }
        }
        
        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        Order savedOrder = orderRepository.save(order);
        
        return mapToResponse(savedOrder);
    }

    private void validateStatusTransition(String currentStatus, String newStatus) {
        // Cannot change status if order is in final state
        if (Order.STATUS_DELIVERED.equals(currentStatus) || 
            Order.STATUS_CANCELLED.equals(currentStatus) || 
            Order.STATUS_RETURNED.equals(currentStatus)) {
            throw new ConflictException("Cannot change status of an order in final state");
        }
        
        // Valid status values
        Set<String> validStatuses = Set.of(
            Order.STATUS_PLACED,
            Order.STATUS_CONFIRMED,
            Order.STATUS_PROCESSING,
            Order.STATUS_SHIPPED,
            Order.STATUS_DELIVERED,
            Order.STATUS_CANCELLED,
            Order.STATUS_RETURNED
        );
        
        if (!validStatuses.contains(newStatus)) {
            throw new ConflictException("Invalid status: " + newStatus);
        }
        
        // Business rules for status transitions
        if (Order.STATUS_PLACED.equals(currentStatus)) {
            // From PLACED, can go to CONFIRMED or CANCELLED
            if (!Order.STATUS_CONFIRMED.equals(newStatus) && 
                !Order.STATUS_CANCELLED.equals(newStatus)) {
                throw new ConflictException(
                    "From PLACED, order can only be CONFIRMED or CANCELLED"
                );
            }
        } else if (Order.STATUS_CONFIRMED.equals(currentStatus)) {
            // From CONFIRMED, can go to PROCESSING or CANCELLED
            if (!Order.STATUS_PROCESSING.equals(newStatus) && 
                !Order.STATUS_CANCELLED.equals(newStatus)) {
                throw new ConflictException(
                    "From CONFIRMED, order can only be PROCESSING or CANCELLED"
                );
            }
        } else if (Order.STATUS_PROCESSING.equals(currentStatus)) {
            // From PROCESSING, can only go to SHIPPED (cannot cancel)
            if (!Order.STATUS_SHIPPED.equals(newStatus)) {
                throw new ConflictException(
                    "From PROCESSING, order can only be SHIPPED"
                );
            }
        } else if (Order.STATUS_SHIPPED.equals(currentStatus)) {
            // From SHIPPED, can go to DELIVERED or RETURNED
            if (!Order.STATUS_DELIVERED.equals(newStatus) && 
                !Order.STATUS_RETURNED.equals(newStatus)) {
                throw new ConflictException(
                    "From SHIPPED, order can only be DELIVERED or RETURNED"
                );
            }
        }
    }
}
