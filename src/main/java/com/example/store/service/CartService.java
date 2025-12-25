package com.example.store.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.CartItemRequest;
import com.example.store.dto.CartItemResponse;
import com.example.store.dto.CartItemUpdateRequest;
import com.example.store.dto.CartResponse;
import com.example.store.exception.ConflictException;
import com.example.store.exception.InsufficientStockException;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Book;
import com.example.store.model.CartItem;
import com.example.store.model.ProductSku;
import com.example.store.model.ShoppingCart;
import com.example.store.model.User;
import com.example.store.repository.BookRepository;
import com.example.store.repository.CartItemRepository;
import com.example.store.repository.InventoryRepository;
import com.example.store.repository.ProductSkuRepository;
import com.example.store.repository.ShoppingCartRepository;
import com.example.store.repository.UserRepository;

@Service
public class CartService {

    private static final String CART_STATUS = "ACTIVE";
    private static final int MAX_QUANTITY_PER_ITEM = 99;
    private static final int MAX_TOTAL_ITEMS = 100;

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final ProductSkuRepository productSkuRepository;
    private final UserRepository userRepository;
    private final InventoryRepository inventoryRepository;

    public CartService(
        ShoppingCartRepository shoppingCartRepository,
        CartItemRepository cartItemRepository,
        BookRepository bookRepository,
        ProductSkuRepository productSkuRepository,
        UserRepository userRepository,
        InventoryRepository inventoryRepository
    ) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.productSkuRepository = productSkuRepository;
        this.userRepository = userRepository;
        this.inventoryRepository = inventoryRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse getCartByUser(String username){
        User user = fetchUser(username);
        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), CART_STATUS)
            .orElseThrow(() -> new ResourceNotFoundException("Shopping cart not found"));

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse addItemToCart(String username, CartItemRequest cartItemRequest) {
        User user = fetchUser(username);
        ProductSku productSku = productSkuRepository.findById(cartItemRequest.skuId())
            .orElseThrow(() -> new ResourceNotFoundException("Product SKU not found"));
        
        Book book = productSku.getBook();

        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), CART_STATUS)
            .orElseGet(() -> createCart(user));

        List<CartItem> cartItems = ensureItemsInitialized(cart);

        CartItem cartItem = cartItemRepository.findByCartAndProductSku(cart, productSku)
            .map(item -> {
                int newQuantity = item.getQuantity() + cartItemRequest.quantity();
                validateQuantity(newQuantity);
                validateStock(productSku, newQuantity);
                item.setQuantity(newQuantity);
                return item;
            })
            .orElseGet(() -> {
                validateQuantity(cartItemRequest.quantity());
                validateStock(productSku, cartItemRequest.quantity());
                return createCartItem(cart, book, productSku, cartItemRequest.quantity());
            });

        boolean isNewItem = cartItem.getId() == null;
        if (isNewItem) {
            cartItems.add(cartItem);
        }

        BigDecimal updatedTotal = cartItems.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        cart.setSubtotal(updatedTotal);
        cart.setTotalItems(totalItems);

        shoppingCartRepository.save(cart);
        if (!isNewItem) {
            cartItemRepository.save(cartItem);
        }

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse updateCartItem(String username, Long itemId, CartItemUpdateRequest request) {
        User user = fetchUser(username);
        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), CART_STATUS)
            .orElseThrow(() -> new ResourceNotFoundException("Shopping cart not found"));

        CartItem cartItem = cartItemRepository.findById(itemId)
            .filter(item -> item.getCart().getId().equals(cart.getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        validateQuantity(request.quantity());
        validateStock(cartItem.getProductSku(), request.quantity());
        cartItem.setQuantity(request.quantity());
        cartItemRepository.save(cartItem);

        recalculateCartTotals(cart);
        shoppingCartRepository.save(cart);

        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse removeCartItem(String username, Long itemId) {
        User user = fetchUser(username);
        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), CART_STATUS)
            .orElseThrow(() -> new ResourceNotFoundException("Shopping cart not found"));

        CartItem cartItem = cartItemRepository.findById(itemId)
            .filter(item -> item.getCart().getId().equals(cart.getId()))
            .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));

        cart.getItems().remove(cartItem);
        cartItemRepository.delete(cartItem);

        recalculateCartTotals(cart);
        shoppingCartRepository.save(cart);
        
        return buildCartResponse(cart);
    }

    @Transactional
    public CartResponse clearCart(String username) {
        User user = fetchUser(username);
        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), CART_STATUS)
            .orElseThrow(() -> new ResourceNotFoundException("Shopping cart not found"));

        cart.getItems().clear();
        cart.setSubtotal(BigDecimal.ZERO);
        cart.setTotalItems(0);
        shoppingCartRepository.save(cart);
        
        return buildCartResponse(cart);
    }

    private ShoppingCart createCart(User user) {
        ShoppingCart cart = ShoppingCart.builder()
            .user(user)
            .status(CART_STATUS)
            .items(new ArrayList<>())
            .build();
        return shoppingCartRepository.save(cart);
    }

    private CartItem createCartItem(ShoppingCart cart, Book book, ProductSku sku, int quantity) {
        BigDecimal unitPrice = sku.resolveCurrentPrice();
        return CartItem.builder()
            .cart(cart)
            .productSku(sku)
            .quantity(quantity)
            .unitPrice(unitPrice)
            .build();
    }

    private CartResponse buildCartResponse(ShoppingCart cart) {
        List<CartItemResponse> items = cart.getItems().stream()
            .map(item -> {
                BigDecimal originalPrice = item.getUnitPrice();
                BigDecimal currentPrice = item.getProductSku().resolveCurrentPrice();
                BigDecimal priceDiff = currentPrice.subtract(originalPrice);
                boolean priceChanged = priceDiff.compareTo(BigDecimal.ZERO) != 0;
                
                return CartItemResponse.builder()
                    .itemId(item.getId())
                    .bookId(item.getProductSku().getBook().getId())
                    .title(item.getProductSku().getBook().getTitle())
                    .sku(item.getProductSku().getSku())
                    .quantity(item.getQuantity())
                    .price(currentPrice)
                    .originalPrice(originalPrice)
                    .priceChanged(priceChanged)
                    .priceDiff(priceChanged ? priceDiff : null)
                    .build();
            })
            .toList();

        return CartResponse.builder()
            .cartId(cart.getId())
            .status(cart.getStatus())
            .totalAmount(cart.getSubtotal())
            .totalItems(cart.getTotalItems())
            .items(items)
            .build();
    }

    private List<CartItem> ensureItemsInitialized(ShoppingCart cart) {
        List<CartItem> items = cart.getItems();
        if (items == null) {
            items = new ArrayList<>();
            cart.setItems(items);
        }
        return items;
    }

    private User fetchUser(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private ProductSku resolveDefaultSku(Book book) {
        if (book.getDefaultSkuId() != null) {
            return productSkuRepository.findById(book.getDefaultSkuId())
                .orElseThrow(() -> new ResourceNotFoundException("Product SKU not found for book"));
        }
        return productSkuRepository.findFirstByBookId(book.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Product SKU not found for book"));
    }

    private void recalculateCartTotals(ShoppingCart cart) {
        List<CartItem> items = cart.getItems();
        BigDecimal subtotal = items.stream()
            .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalItems = items.stream().mapToInt(CartItem::getQuantity).sum();
        
        if (totalItems > MAX_TOTAL_ITEMS) {
            throw new ConflictException("Cart cannot exceed " + MAX_TOTAL_ITEMS + " total items");
        }
        
        cart.setSubtotal(subtotal);
        cart.setTotalItems(totalItems);
    }

    private void validateQuantity(int quantity) {
        if (quantity < 1) {
            throw new ConflictException("Quantity must be at least 1");
        }
        if (quantity > MAX_QUANTITY_PER_ITEM) {
            throw new ConflictException("Quantity cannot exceed " + MAX_QUANTITY_PER_ITEM + " per item");
        }
    }

    private void validateStock(ProductSku productSku, int requestedQuantity) {
        var inventory = inventoryRepository.findByProductSkuId(productSku.getId())
            .orElse(null);
        
        int availableStock = 0;
        if (inventory != null) {
            int totalStock = inventory.getStock() != null ? inventory.getStock() : 0;
            int reserved = inventory.getReserved() != null ? inventory.getReserved() : 0;
            availableStock = totalStock - reserved;
        }
        
        if (availableStock < requestedQuantity) {
            throw new InsufficientStockException(
                "Insufficient stock for SKU " + productSku.getSku() + 
                ". Available: " + availableStock + ", requested: " + requestedQuantity
            );
        }
    }
}
