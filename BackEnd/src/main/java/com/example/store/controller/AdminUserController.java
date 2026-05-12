package com.example.store.controller;

import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.store.dto.CursorPageResponse;
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
     * Query params: role (ADMIN/CUSTOMER), status (ACTIVE/INACTIVE), username
     */
    @GetMapping
    public ResponseEntity<Page<UserListResponse>> getAllUsers(
        @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
        @RequestParam(required = false) String role,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) String username
    ) {
        Page<UserListResponse> users = userService.getAllUsers(pageable, role, status, username);
        return ResponseEntity.ok(users);
    }

    /**
     * GET /admin/users/cursor
     * Keyset (cursor) pagination — no COUNT(*) overhead, O(1) per page at any depth.
     *
     * First page: omit lastId and lastCreatedAt.
     * Subsequent pages: pass nextLastId and nextLastCreatedAt from the previous response.
     * Sort is always created_at DESC, id DESC.
     */
    @GetMapping("/cursor")
    public ResponseEntity<CursorPageResponse<UserListResponse>> getAllUsersCursor(
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long lastId,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime lastCreatedAt,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String username
    ) {
        int cappedSize = Math.min(Math.max(size, 1), 100);
        CursorPageResponse<UserListResponse> response =
                userService.getAllUsersCursor(cappedSize, lastId, lastCreatedAt, role, status, username);
        return ResponseEntity.ok(response);
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
