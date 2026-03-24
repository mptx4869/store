package com.example.store.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.UserDetailResponse;
import com.example.store.dto.UserListResponse;
import com.example.store.dto.UserRoleUpdateRequest;
import com.example.store.dto.UserStatusUpdateRequest;
import com.example.store.service.UserService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * GET /admin/users
     * Get paginated list of users with optional filtering
     * Query params: role (ADMIN/CUSTOMER), status (ACTIVE/INACTIVE)
     */
    @GetMapping
    public ResponseEntity<Page<UserListResponse>> getAllUsers(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status
    ) {
        Page<UserListResponse> users = userService.getAllUsers(pageable, role, status);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /admin/users/{userId}
     * Get detailed information about a specific user
     */
    @GetMapping("/{userId}")
    public ResponseEntity<UserDetailResponse> getUserById(@PathVariable Long userId) {
        UserDetailResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }

    /**
     * PATCH /admin/users/{userId}/status
     * Update user status (ACTIVE/INACTIVE)
     */
    @PatchMapping("/{userId}/status")
    public ResponseEntity<UserListResponse> updateUserStatus(
        @PathVariable Long userId,
        @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        UserListResponse user = userService.updateUserStatus(userId, request.getStatus().toUpperCase());
        return ResponseEntity.ok(user);
    }

    /**
     * PATCH /admin/users/{userId}/role
     * Update user role (ADMIN/CUSTOMER)
     */
    @PatchMapping("/{userId}/role")
    public ResponseEntity<UserListResponse> updateUserRole(
        @PathVariable Long userId,
        @Valid @RequestBody UserRoleUpdateRequest request
    ) {
        UserListResponse user = userService.updateUserRole(userId, request.getRole().toUpperCase());
        return ResponseEntity.ok(user);
    }
}
