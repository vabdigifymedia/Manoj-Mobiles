package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.AgentLocationTracking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AgentLocationTrackingRepository extends JpaRepository<AgentLocationTracking, UUID> {
    List<AgentLocationTracking> findByOrderIdOrderByRecordedAtDesc(UUID orderId);
    Optional<AgentLocationTracking> findFirstByOrderIdOrderByRecordedAtDesc(UUID orderId); // latest ping
}