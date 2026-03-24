package com.example.store.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.CartItemRequest;
import com.example.store.dto.CartItemUpdateRequest;
import com.example.store.dto.CartResponse;
import com.example.store.service.CartService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping()
    public ResponseEntity<CartResponse> getCart(Authentication authentication) {
        CartResponse response = cartService.getCartByUser(authentication.getName());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/items")
    public ResponseEntity<CartResponse> addItem(
        @Valid @RequestBody CartItemRequest cartItemRequest,
        Authentication authentication
    ) {
        CartResponse response = cartService.addItemToCart(authentication.getName(), cartItemRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> updateItem(
        @PathVariable Long itemId,
        @Valid @RequestBody CartItemUpdateRequest request,
        Authentication authentication
    ) {
        CartResponse response = cartService.updateCartItem(authentication.getName(), itemId, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/items/{itemId}")
    public ResponseEntity<CartResponse> removeItem(
        @PathVariable Long itemId,
        Authentication authentication
    ) {
        CartResponse response = cartService.removeCartItem(authentication.getName(), itemId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping()
    public ResponseEntity<CartResponse> clearCart(Authentication authentication) {
        CartResponse response = cartService.clearCart(authentication.getName());
        return ResponseEntity.ok(response);
    }
}