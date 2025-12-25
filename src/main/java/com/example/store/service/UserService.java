package com.example.store.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.UserDetailResponse;
import com.example.store.dto.UserListResponse;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Order;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

/**
 * Service for managing users - CRUD operations and admin functions
 * Separated from authentication concerns (AutherService)
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, OrderRepository orderRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.orderRepository = orderRepository;
    }

    // ==================== ADMIN USER MANAGEMENT ====================

    public Page<UserListResponse> getAllUsers(Pageable pageable, String roleFilter, String statusFilter) {
        Page<User> users;
        
        if (roleFilter != null && statusFilter != null) {
            users = userRepository.findByRoleNameAndStatus(
                roleFilter.toUpperCase(), 
                statusFilter.toUpperCase(), 
                pageable
            );
        } else if (roleFilter != null) {
            users = userRepository.findByRoleName(roleFilter.toUpperCase(), pageable);
        } else if (statusFilter != null) {
            users = userRepository.findByStatus(statusFilter.toUpperCase(), pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        
        return users.map(this::mapToUserListResponse);
    }

    public UserDetailResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        return mapToUserDetailResponse(user);
    }

    @Transactional
    public UserListResponse updateUserStatus(Long userId, String status) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        String upperStatus = status.toUpperCase();
        if (!upperStatus.equals("ACTIVE") && !upperStatus.equals("INACTIVE")) {
            throw new IllegalArgumentException("Status must be either ACTIVE or INACTIVE");
        }
        
        user.setStatus(upperStatus);
        userRepository.save(user);
        
        return mapToUserListResponse(user);
    }

    
    @Transactional
    public UserListResponse updateUserRole(Long userId, String roleName) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        
        Role role = roleRepository.findByName(roleName.toUpperCase())
            .orElseThrow(() -> new ResourceNotFoundException("Role not found"));
        
        user.setRole(role);
        userRepository.save(user);
        
        return mapToUserListResponse(user);
    }

    // ==================== HELPER METHODS ====================

    /**
     * Find user by username
     */
    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    /**
     * Check if username exists
     */
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    /**
     * Check if email exists
     */
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    /**
     * Save user
     */
    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    /**
     * Delete user
     */
    @Transactional
    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    // ==================== MAPPING METHODS ====================

    /**
     * Map User entity to UserListResponse DTO
     */
    private UserListResponse mapToUserListResponse(User user) {
        Integer orderCount = orderRepository.countByUserId(user.getId());
        
        return UserListResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().getName())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .lastLogin(null) // TODO: implement last login tracking
            .totalOrders(orderCount)
            .build();
    }

    /**
     * Map User entity to UserDetailResponse DTO with full statistics
     */
    private UserDetailResponse mapToUserDetailResponse(User user) {
        List<Order> orders = orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        
        // Calculate order statistics
        Integer totalOrders = orders.size();
        Integer completedOrders = (int) orders.stream()
            .filter(o -> "COMPLETED".equals(o.getStatus()))
            .count();
        Integer cancelledOrders = (int) orders.stream()
            .filter(o -> "CANCELLED".equals(o.getStatus()))
            .count();
        
        // Calculate total spending from completed orders
        BigDecimal totalSpent = orders.stream()
            .filter(o -> "COMPLETED".equals(o.getStatus()))
            .map(Order::getTotalAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Get recent orders (last 5)
        List<UserDetailResponse.UserOrderSummary> recentOrders = orders.stream()
            .limit(5)
            .map(order -> UserDetailResponse.UserOrderSummary.builder()
                .orderId(order.getId())
                .status(order.getStatus())
                .total(order.getTotalAmount().toString())
                .createdAt(order.getCreatedAt())
                .build())
            .collect(Collectors.toList());
        
        return UserDetailResponse.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .role(user.getRole().getName())
            .status(user.getStatus())
            .createdAt(user.getCreatedAt())
            .updatedAt(user.getUpdatedAt())
            .lastLogin(null) // TODO: implement last login tracking
            .totalOrders(totalOrders)
            .completedOrders(completedOrders)
            .cancelledOrders(cancelledOrders)
            .totalSpent(totalSpent.toString())
            .recentOrders(recentOrders)
            .build();
    }
}
