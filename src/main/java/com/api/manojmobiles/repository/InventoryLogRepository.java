package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.InventoryLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InventoryLogRepository extends JpaRepository<InventoryLog, UUID> {
    List<InventoryLog> findByVariantIdOrderByCreatedAtDesc(UUID variantId);
}
