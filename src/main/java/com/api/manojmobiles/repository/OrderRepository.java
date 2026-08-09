package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Order;
import com.api.manojmobiles.entity.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUserIdOrderByPlacedAtDesc(UUID userId, Pageable pageable);
    boolean existsByOrderNumber(String orderNumber);

    // Total revenue (excluding CANCELLED and RETURNED orders)
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.orderStatus NOT IN :excludedStatuses")
    BigDecimal calculateTotalRevenue(@Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    // Order count grouped by status (for pie chart)
    @Query("SELECT o.orderStatus AS status, COUNT(o) AS count FROM Order o GROUP BY o.orderStatus")
    List<Object[]> getOrderCountByStatus();

    // Daily sales revenue for last N days (for line chart)
    @Query("SELECT CAST(o.placedAt AS date) AS orderDate, COALESCE(SUM(o.totalAmount), 0) AS revenue " +
            "FROM Order o WHERE o.placedAt >= :since AND o.orderStatus NOT IN :excludedStatuses " +
            "GROUP BY CAST(o.placedAt AS date) ORDER BY CAST(o.placedAt AS date) ASC")
    List<Object[]> getDailySales(@Param("since") LocalDateTime since, @Param("excludedStatuses") List<OrderStatus> excludedStatuses);

    // Recent 5 orders
    List<Order> findTop5ByOrderByPlacedAtDesc();
}