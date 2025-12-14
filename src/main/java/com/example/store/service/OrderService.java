package com.example.store.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.OrderCreateRequest;
import com.example.store.dto.OrderItemResponse;
import com.example.store.dto.OrderResponse;
import com.example.store.exception.ConflictException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Address;
import com.example.store.model.CartItem;
import com.example.store.model.Order;
import com.example.store.model.OrderItem;
import com.example.store.model.ProductSku;
import com.example.store.model.ShoppingCart;
import com.example.store.model.User;
import com.example.store.repository.AddressRepository;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.ShoppingCartRepository;
import com.example.store.repository.UserRepository;

@Service
public class OrderService {

    private static final String CART_STATUS_ACTIVE = "ACTIVE";
    private static final String CART_STATUS_COMPLETED = "COMPLETED";
    private static final String ORDER_STATUS_PLACED = "PLACED";
    private static final String DEFAULT_CURRENCY = "USD";

    private final OrderRepository orderRepository;
    private final ShoppingCartRepository shoppingCartRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;

    public OrderService(
        OrderRepository orderRepository,
        ShoppingCartRepository shoppingCartRepository,
        UserRepository userRepository,
        AddressRepository addressRepository
    ) {
        this.orderRepository = orderRepository;
        this.shoppingCartRepository = shoppingCartRepository;
        this.userRepository = userRepository;
        this.addressRepository = addressRepository;
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

        Long shippingAddressId = resolveAddressId(user, request != null ? request.shippingAddressId() : null);
        Long billingAddressId = resolveAddressId(user, request != null ? request.billingAddressId() : null);

        BigDecimal totalAmount = cartItems.stream()
            .map(this::calculateLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        Order order = Order.builder()
            .user(user)
            .cart(cart)
            .status(ORDER_STATUS_PLACED)
            .currency(resolveCurrency(request))
            .totalAmount(totalAmount)
            .shippingAddressId(shippingAddressId)
            .billingAddressId(billingAddressId)
            .placedAt(LocalDateTime.now())
            .build();

        cartItems.forEach(cartItem -> {
            ProductSku productSku = cartItem.getProductSku();
            if (productSku == null || productSku.getBook() == null) {
                throw new ConflictException("Cart item is missing product details.");
            }
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

    private Long resolveAddressId(User user, Long addressId) {
        if (addressId == null) {
            return null;
        }
        Address address = addressRepository.findById(addressId)
            .filter(entity -> entity.getUser() != null && entity.getUser().getId().equals(user.getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        return address.getId();
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
            .shippingAddressId(order.getShippingAddressId())
            .billingAddressId(order.getBillingAddressId())
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
}
