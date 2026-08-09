package com.api.manojmobiles.repository;

import com.api.manojmobiles.entity.ServiceablePincode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ServiceablePincodeRepository extends JpaRepository<ServiceablePincode, UUID> {
    Optional<ServiceablePincode> findByPincode(String pincode);
    List<ServiceablePincode> findByCityId(UUID cityId);
    boolean existsByPincode(String pincode);
}
