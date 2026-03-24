package com.example.store.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.store.dto.RevenueResponse;
import com.example.store.model.Order;
import com.example.store.repository.OrderRepository;

@Service
public class AnalyticsService {

    private final OrderRepository orderRepository;

    public AnalyticsService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Transactional(readOnly = true)
    public List<RevenueResponse> getRevenueAnalytics(LocalDateTime startDate, LocalDateTime endDate, String groupBy) {
        List<Order> completedOrders = orderRepository.findCompletedOrdersBetweenDates(startDate, endDate);
        
        Map<String, AggregatedData> aggregationMap = new LinkedHashMap<>();
        
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM");
        WeekFields weekFields = WeekFields.of(Locale.getDefault());

        for (Order order : completedOrders) {
            LocalDateTime placedAt = order.getPlacedAt();
            String periodKey = "";
            
            if ("MONTH".equalsIgnoreCase(groupBy)) {
                periodKey = placedAt.format(monthFormatter);
            } else if ("WEEK".equalsIgnoreCase(groupBy)) {
                int weekNum = placedAt.get(weekFields.weekOfWeekBasedYear());
                int year = placedAt.getYear();
                periodKey = year + "-W" + String.format("%02d", weekNum);
            } else { // default DAY
                periodKey = placedAt.format(dayFormatter);
            }
            
            AggregatedData data = aggregationMap.computeIfAbsent(periodKey, k -> new AggregatedData());
            data.totalRevenue = data.totalRevenue.add(order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO);
            data.orderCount++;
        }
        
        List<RevenueResponse> result = new ArrayList<>();
        for (Map.Entry<String, AggregatedData> entry : aggregationMap.entrySet()) {
            result.add(new RevenueResponse(entry.getKey(), entry.getValue().totalRevenue, entry.getValue().orderCount));
        }
        
        result.sort((r1, r2) -> r1.period().compareTo(r2.period()));
        
        return result;
    }
    
    private static class AggregatedData {
        BigDecimal totalRevenue = BigDecimal.ZERO;
        int orderCount = 0;
    }
}
