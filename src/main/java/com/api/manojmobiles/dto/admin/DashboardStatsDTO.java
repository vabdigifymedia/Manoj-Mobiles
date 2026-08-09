package com.api.manojmobiles.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DashboardStatsDTO {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalProducts;
    private long totalCustomers;
    private long lowStockCount;

    private List<SalesChartDataDTO> salesChart;
    private List<OrderStatusDistributionDTO> orderStatusDistribution;
    private List<RecentOrderDTO> recentOrders;
}
