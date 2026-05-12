package com.example.store.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.CursorPageResponse;
import com.example.store.dto.UserDetailResponse;
import com.example.store.dto.UserListResponse;
import com.example.store.exception.ResourceNotFoundException;
import com.example.store.model.Role;
import com.example.store.model.User;
import com.example.store.repository.OrderRepository;
import com.example.store.repository.RoleRepository;
import com.example.store.repository.UserRepository;

import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.store.dto.UserChangePasswordRequest;
import com.example.store.exception.LoginException;

/**
 * Service for managing users - CRUD operations and admin functions.
 * Separated from authentication concerns (AutherService).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final OrderRepository orderRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository,
                       OrderRepository orderRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.orderRepository = orderRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // ==================== ADMIN USER MANAGEMENT ====================

    /**
     * Returns a paginated, filtered user list.
     *
     * Sort is driven directly by {@code pageable} — Spring Data JPA translates it
     * to an explicit ORDER BY, allowing PostgreSQL to use column indexes instead of
     * the previous CASE WHEN filesort approach that was unusable at 1 M+ rows.
     *
     * totalOrders is read from the denormalized {@code users.total_orders} column,
     * so no JOIN to the orders table is needed here.
     */
    public Page<UserListResponse> getAllUsers(
            Pageable pageable,
            String roleFilter,
            String statusFilter,
            String username) {

        String roleName         = roleFilter != null ? roleFilter.toUpperCase() : null;
        String status           = statusFilter != null ? statusFilter.toUpperCase() : null;
        String usernamePattern  = (username != null && !username.isBlank())
                ? ("%" + username.trim().toLowerCase() + "%")
                : null;

        Page<UserRepository.AdminUserListRow> rows = userRepository.findAdminUserList(
                roleName, status, usernamePattern, normalizeUserPageable(pageable));

        return rows.map(this::mapToUserListResponse);
    }

    /**
     * Keyset (cursor) variant of the admin user list.
     *
     * Sort is always {@code created_at DESC, id DESC}.  Pass {@code lastId}
     * and {@code lastCreatedAt} from the previous response to continue paging;
     * omit both for the first page.
     */
    public CursorPageResponse<UserListResponse> getAllUsersCursor(
            int size,
            Long lastId,
            LocalDateTime lastCreatedAt,
            String roleFilter,
            String statusFilter,
            String username) {

        String roleName = roleFilter != null ? roleFilter.toUpperCase() : null;
        String status   = statusFilter != null ? statusFilter.toUpperCase() : null;
        // Pre-build pattern avoids CONCAT in JPQL (null CONCAT triggers lower(bytea) bug)
        String usernamePattern = (username != null && !username.isBlank())
                ? ("%" + username.trim().toLowerCase() + "%")
                : null;

        List<UserListResponse> content;
        boolean hasNext;

        if (lastId == null || lastCreatedAt == null) {
            // First page: reuse the offset-based query — no LocalDateTime param,
            // so no null-timestamp/bytea type-inference problem.
            Pageable firstPage = PageRequest.of(0, size, Sort.by(Sort.Direction.DESC, "createdAt"));
            Page<UserListResponse> page = getAllUsers(firstPage, roleFilter, statusFilter, username);
            content  = page.getContent();
            hasNext  = page.hasNext();
        } else {
            // Subsequent pages: cursor is always non-null — safe to pass LocalDateTime.
            Pageable pageable = PageRequest.of(0, size);
            Slice<UserRepository.AdminUserListRow> slice = userRepository.findAdminUserListKeysetNext(
                    roleName, status, usernamePattern, lastCreatedAt, lastId, pageable);
            content  = slice.getContent().stream().map(this::mapToUserListResponse).toList();
            hasNext  = slice.hasNext();
        }

        Long nextLastId = null;
        LocalDateTime nextLastCreatedAt = null;
        if (hasNext && !content.isEmpty()) {
            UserListResponse last = content.get(content.size() - 1);
            nextLastId          = last.getId();
            nextLastCreatedAt   = last.getCreatedAt();
        }

        return CursorPageResponse.<UserListResponse>builder()
                .content(content)
                .hasNext(hasNext)
                .nextLastId(nextLastId)
                .nextLastCreatedAt(nextLastCreatedAt)
                .build();
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

    @Transactional
    public void changePassword(String username, UserChangePasswordRequest request) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPasswordHash())) {
            throw new LoginException("Old password is incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    public User findByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Transactional
    public User saveUser(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void deleteUser(User user) {
        userRepository.delete(user);
    }

    // ==================== MAPPING METHODS ====================

    /**
     * Maps an AdminUserListRow projection to UserListResponse.
     * totalOrders comes from the denormalized column — no extra DB call.
     */
    private UserListResponse mapToUserListResponse(UserRepository.AdminUserListRow row) {
        return UserListResponse.builder()
                .id(row.getId())
                .username(row.getUsername())
                .email(row.getEmail())
                .role(row.getRole())
                .status(row.getStatus())
                .createdAt(row.getCreatedAt())
                .lastLogin(null) // TODO: implement last login tracking
                .totalOrders(row.getTotalOrders() != null ? row.getTotalOrders().intValue() : 0)
                .build();
    }

    /**
     * Maps a User entity to UserListResponse.
     * Used by updateUserStatus / updateUserRole which already hold the entity.
     * Reads totalOrders from the entity field — no extra DB call.
     */
    private UserListResponse mapToUserListResponse(User user) {
        return UserListResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole().getName())
                .status(user.getStatus())
                .createdAt(user.getCreatedAt())
                .lastLogin(null) // TODO: implement last login tracking
                .totalOrders(user.getTotalOrders() != null ? user.getTotalOrders() : 0)
                .build();
    }

    /**
     * Maps a User entity to UserDetailResponse with full order statistics.
     *
     * Two targeted queries replace the previous load-all-orders approach:
     *   1. findUserOrderStatsByUserId — single aggregate row (COUNT/SUM at DB level)
     *   2. findRecentOrdersByUserId   — 5 most recent orders, projection only
     *
     * For a user with 5 000 orders this avoids loading ~5 000 Order objects into heap.
     */
    private UserDetailResponse mapToUserDetailResponse(User user) {
        // Aggregate stats — always returns exactly 1 row (COUNT(*) is never empty)
        OrderRepository.UserOrderStats stats =
                orderRepository.findUserOrderStatsByUserId(user.getId());

        // 5 most recent orders — projection, no orderItems loaded
        List<OrderRepository.RecentOrderSummary> recent =
                orderRepository.findRecentOrdersByUserId(user.getId(), PageRequest.of(0, 5));

        List<UserDetailResponse.UserOrderSummary> recentOrders = recent.stream()
                .map(o -> UserDetailResponse.UserOrderSummary.builder()
                        .orderId(o.getId())
                        .status(o.getStatus())
                        .total(o.getTotalAmount().toString())
                        .createdAt(o.getCreatedAt())
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
                .totalOrders(stats.getTotalOrders().intValue())
                .completedOrders(stats.getCompletedOrders().intValue())
                .cancelledOrders(stats.getCancelledOrders().intValue())
                .totalSpent(stats.getTotalSpent().toString())
                .recentOrders(recentOrders)
                .build();
    }

    /**
     * Rebuilds a Pageable with validated sort properties so that an unknown
     * sort field from the request cannot reach JPQL and cause a query error.
     * Valid properties map directly to User entity field names.
     */
    private Pageable normalizeUserPageable(Pageable pageable) {
        if (!pageable.getSort().isSorted()) {
            return pageable;
        }
        List<Sort.Order> normalized = pageable.getSort().stream()
                .map(o -> {
                    String prop = switch (o.getProperty()) {
                        case "username"  -> "username";
                        case "email"     -> "email";
                        case "status"    -> "status";
                        default          -> "createdAt";
                    };
                    return new Sort.Order(o.getDirection(), prop);
                })
                .toList();
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(normalized));
    }
}
