package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    Page<Order> findByUserIdOrderByPlacedAtDesc(UUID userId, Pageable pageable);
    boolean existsByOrderNumber(String orderNumber);
}