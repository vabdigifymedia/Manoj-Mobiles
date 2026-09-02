package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.BulkEnquiry;
import com.api.manojmobiles.entity.enums.EnquiryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface BulkEnquiryRepository extends JpaRepository<BulkEnquiry, UUID> {
    Page<BulkEnquiry> findByStatusOrderByCreatedAtDesc(EnquiryStatus status, Pageable pageable);
    Page<BulkEnquiry> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
