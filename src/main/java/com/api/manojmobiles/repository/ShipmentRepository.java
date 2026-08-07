package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ShipmentRepository extends JpaRepository<Shipment, UUID> {
    Optional<Shipment> findByOrderId(UUID orderId);
    List<Shipment> findByAgentId(UUID agentId);
    org.springframework.data.domain.Page<Shipment> findByAgentIdAndCurrentStatusIn(UUID agentId, List<com.api.manojmobiles.entity.enums.OrderStatus> statuses, org.springframework.data.domain.Pageable pageable);
}
