package com.example.store.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.OrderCreateRequest;
import com.example.store.dto.OrderResponse;
import com.example.store.service.OrderService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<List<OrderResponse>> getOrderHistory(Authentication authentication) {
        List<OrderResponse> history = orderService.getOrderHistory(authentication.getName());
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(
        @PathVariable Long orderId,
        Authentication authentication
    ) {
        OrderResponse order = orderService.getOrderById(authentication.getName(), orderId);
        return ResponseEntity.ok(order);
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
        @Valid @RequestBody(required = false) OrderCreateRequest request,
        Authentication authentication
    ) {
        OrderResponse response = orderService.createOrder(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PatchMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
        @PathVariable Long orderId,
        Authentication authentication
    ) {
        OrderResponse response = orderService.cancelOrder(authentication.getName(), orderId);
        return ResponseEntity.ok(response);
    }
}
