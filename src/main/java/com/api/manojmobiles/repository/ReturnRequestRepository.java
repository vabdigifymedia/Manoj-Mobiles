package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.ReturnRequest;
import com.api.manojmobiles.entity.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, UUID> {
    List<ReturnRequest> findByOrderItemId(UUID orderItemId);
    List<ReturnRequest> findByStatus(ReturnStatus status);
}