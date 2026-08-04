package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.DeliveryAgent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface DeliveryAgentRepository extends JpaRepository<DeliveryAgent, UUID> {
    Optional<DeliveryAgent> findByUserId(UUID userId);
    List<DeliveryAgent> findByIsAvailableTrue();
}