package com.example.store.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailResponse {
    private Long id;
    private String username;
    private String email;
    private String role;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastLogin;
    
    // Order statistics
    private Integer totalOrders;
    private Integer completedOrders;
    private Integer cancelledOrders;
    private String totalSpent;
    
    // Recent activity
    private List<UserOrderSummary> recentOrders;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserOrderSummary {
        private Long orderId;
        private String status;
        private String total;
        private LocalDateTime createdAt;
    }
}
