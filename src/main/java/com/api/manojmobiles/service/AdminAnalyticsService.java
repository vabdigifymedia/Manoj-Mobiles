package com.api.manojmobiles.service;

import com.api.manojmobiles.dto.admin.*;
import com.api.manojmobiles.entity.Order;
import com.api.manojmobiles.entity.enums.OrderStatus;
import com.api.manojmobiles.entity.enums.Role;
import com.api.manojmobiles.entity.enums.StockStatus;
import com.api.manojmobiles.repository.OrderRepository;
import com.api.manojmobiles.repository.ProductRepository;
import com.api.manojmobiles.repository.ProductVariantRepository;
import com.api.manojmobiles.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAnalyticsService {

        private final OrderRepository orderRepository;
        private final UserRepository userRepository;
        private final ProductRepository productRepository;
        private final ProductVariantRepository productVariantRepository;

        private static final List<OrderStatus> EXCLUDED_STATUSES = Arrays.asList(OrderStatus.CANCELLED,
                        OrderStatus.RETURNED);

        @Transactional(readOnly = true)
        public DashboardStatsDTO getDashboardStats() {
                log.info("Fetching admin dashboard analytics");

                // 1. Total Revenue (excluding cancelled & returned)
                BigDecimal totalRevenue = orderRepository.calculateTotalRevenue(EXCLUDED_STATUSES);

                // 2. Total Orders
                long totalOrders = orderRepository.count();

                // 3. Total Products
                long totalProducts = productRepository.count();

                // 4. Total Customers (only CUSTOMER role)
                long totalCustomers = userRepository.countByRole(Role.CUSTOMER);

                // 5. Low Stock Count (LIMITED_STOCK or OUT_OF_STOCK)
                long lowStockCount = productVariantRepository.countByStockStatusIn(
                                Arrays.asList(StockStatus.LIMITED_STOCK, StockStatus.OUT_OF_STOCK));

                // 6. Sales Chart Data (last 30 days)
                LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
                List<Object[]> rawSalesData = orderRepository.getDailySales(thirtyDaysAgo, EXCLUDED_STATUSES);
                List<SalesChartDataDTO> salesChart = rawSalesData.stream()
                                .map(row -> SalesChartDataDTO.builder()
                                                .date(row[0].toString())
                                                .revenue((BigDecimal) row[1])
                                                .build())
                                .collect(Collectors.toList());

                // 7. Order Status Distribution (for pie chart)
                List<Object[]> rawStatusData = orderRepository.getOrderCountByStatus();
                List<OrderStatusDistributionDTO> statusDistribution = rawStatusData.stream()
                                .map(row -> OrderStatusDistributionDTO.builder()
                                                .status(((OrderStatus) row[0]).name())
                                                .count((Long) row[1])
                                                .build())
                                .collect(Collectors.toList());

                // 8. Recent 5 Orders
                List<Order> recentOrderEntities = orderRepository.findTop5ByOrderByPlacedAtDesc();
                List<RecentOrderDTO> recentOrders = recentOrderEntities.stream()
                                .map(order -> RecentOrderDTO.builder()
                                                .orderNumber(order.getOrderNumber())
                                                .customerName(order.getUser().getName())
                                                .totalAmount(order.getTotalAmount())
                                                .orderStatus(order.getOrderStatus().name())
                                                .placedAt(order.getPlacedAt())
                                                .build())
                                .collect(Collectors.toList());

                return DashboardStatsDTO.builder()
                                .totalRevenue(totalRevenue)
                                .totalOrders(totalOrders)
                                .totalProducts(totalProducts)
                                .totalCustomers(totalCustomers)
                                .lowStockCount(lowStockCount)
                                .salesChart(salesChart)
                                .orderStatusDistribution(statusDistribution)
                                .recentOrders(recentOrders)
                                .build();
        }
}
