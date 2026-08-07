package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AddressRepository extends JpaRepository<Address, UUID> {
    List<Address> findByUserId(UUID userId);
    org.springframework.data.domain.Page<Address> findByUserId(UUID userId, org.springframework.data.domain.Pageable pageable);
    Optional<Address> findByUserIdAndIsDefaultTrue(UUID userId);
}
