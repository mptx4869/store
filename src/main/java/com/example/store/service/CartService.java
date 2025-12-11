package com.example.store.service;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.CartItemRequest;
import com.example.store.dto.CartItemResponse;
import com.example.store.dto.CartResponse;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Book;
import com.example.store.model.CartItem;
import com.example.store.model.ProductSku;
import com.example.store.model.ShoppingCart;
import com.example.store.model.User;
import com.example.store.repository.BookRepository;
import com.example.store.repository.CartItemRepository;
import com.example.store.repository.ProductSkuRepository;
import com.example.store.repository.ShoppingCartRepository;
import com.example.store.repository.UserRepository;

@Service
public class CartService {

    private static final String CART_STATUS = "ACTIVE";

    private final ShoppingCartRepository shoppingCartRepository;
    private final CartItemRepository cartItemRepository;
    private final BookRepository bookRepository;
    private final ProductSkuRepository productSkuRepository;
    private final UserRepository userRepository;

    public CartService(
        ShoppingCartRepository shoppingCartRepository,
        CartItemRepository cartItemRepository,
        BookRepository bookRepository,
        ProductSkuRepository productSkuRepository,
        UserRepository userRepository
    ) {
        this.shoppingCartRepository = shoppingCartRepository;
        this.cartItemRepository = cartItemRepository;
        this.bookRepository = bookRepository;
        this.productSkuRepository = productSkuRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CartResponse addItemToCart(String username, CartItemRequest cartItemRequest) {
        User user = fetchUser(username);
        Book book = bookRepository.findById(cartItemRequest.bookId())
            .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        ProductSku productSku = resolveDefaultSku(book);

        ShoppingCart cart = shoppingCartRepository.findByUserIdAndStatus(user.getId(), CART_STATUS)
            .orElseGet(() -> createCart(user));

        Set<CartItem> cartItems = ensureItemsInitialized(cart);

        CartItem cartItem = cartItemRepository.findByCartAndProductSku(cart, productSku)
            .map(item -> {
                item.setQuantity(item.getQuantity() + cartItemRequest.quantity());
                return item;
            })
            .orElseGet(() -> createCartItem(cart, book, productSku, cartItemRequest.quantity()));

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

    private ShoppingCart createCart(User user) {
        ShoppingCart cart = ShoppingCart.builder()
            .user(user)
            .status(CART_STATUS)
            .items(new HashSet<>())
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
            .map(item -> CartItemResponse.builder()
                .bookId(item.getProductSku().getBook().getId())
                .title(item.getProductSku().getBook().getTitle())
                .sku(item.getProductSku().getSku())
                .quantity(item.getQuantity())
                .price(item.getUnitPrice())
                .build())
            .toList();

        return CartResponse.builder()
            .cartId(cart.getId())
            .status(cart.getStatus())
            .totalAmount(cart.getSubtotal())
            .totalItems(cart.getTotalItems())
            .items(items)
            .build();
    }

    private Set<CartItem> ensureItemsInitialized(ShoppingCart cart) {
        Set<CartItem> items = cart.getItems();
        if (items == null) {
            items = new HashSet<>();
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
}
